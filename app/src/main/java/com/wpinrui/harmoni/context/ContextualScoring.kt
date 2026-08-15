package com.wpinrui.harmoni.context

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Everything the rules need to know about right now, gathered once per summon. */
data class ContextSnapshot(
    val now: Instant,
    val zone: ZoneId,
    val motion: MotionState,
    val usbDataConnected: Boolean,
    /** Recent visits, newest first, covering at least the longest pull window. */
    val sessions: List<AppSession>,
    /** When each installed app was last used, going back months. */
    val lastUsed: Map<String, Instant>,
    /** Apps with a pending notification that is not ongoing. */
    val notified: Set<String>,
    /** Apps with an ongoing notification, meaning something is in progress. */
    val sticky: Set<String>,
    /** Already reachable from the home surface, so never worth a ring slot. */
    val excluded: Set<String>,
    /**
     * Packages the launcher can actually open.
     *
     * Usage stats report every package that has run, most of which have no launcher activity, so
     * a slot filled from them unchecked draws an empty circle that does nothing when tapped.
     */
    val launchable: Set<String>,
)

/** An app and why it earned its place, kept together so a ring can be explained after the fact. */
data class ScoredApp(val packageName: String, val score: Int, val reasons: List<String>)

/**
 * Turns a snapshot into the eight apps the contextual ring should show.
 *
 * Scores are additive and the arithmetic is deliberately dull: a baseline, then whatever pulls,
 * boosts and penalties currently apply. Nothing multiplies, nothing decays smoothly. A pull is
 * flat for its window and then gone, which makes any given ring explainable by pointing at a
 * handful of numbers.
 */
object ContextualScoring {

    /** The eight to show, best first, filled out with recent apps if the rules are quiet. */
    fun ring(snapshot: ContextSnapshot, size: Int = 8): List<String> {
        val scored = score(snapshot)
            .filter { it.score > 0 }
            .sortedWith(
                compareByDescending<ScoredApp> { it.score }
                    .thenByDescending { snapshot.lastUsed[it.packageName] ?: Instant.EPOCH }
                    .thenBy { it.packageName },
            )
            .map { it.packageName }
            .take(size)

        if (scored.size == size) return scored

        // Nothing scored, or not enough did. Rather than show a short ring, whose positions would
        // shift from summon to summon, fill from what was used most recently.
        val filler = snapshot.lastUsed.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { it in snapshot.launchable }
            .filterNot { it in snapshot.excluded || it in scored }

        return (scored + filler).take(size)
    }

    /** Every candidate with a non-zero score, for the ring and for explaining it. */
    fun score(snapshot: ContextSnapshot): List<ScoredApp> {
        val candidates = (ContextApps.all + snapshot.sticky + snapshot.notified)
            .toSet()
            .filterNot { it in snapshot.excluded }

        return candidates.mapNotNull { packageName ->
            val parts = mutableListOf<Pair<String, Int>>()

            ContextualRules.baseline(packageName, snapshot.motion)
                .takeIf { it != 0 }
                ?.let { parts += "baseline" to it }

            ContextualRules.motionBoost(packageName, snapshot.motion)
                .takeIf { it != 0 }
                ?.let { parts += "motion ${snapshot.motion.name.lowercase()}" to it }

            parts += pullsFor(packageName, snapshot)
            parts += trustBranch(packageName, snapshot)
            parts += rampFor(packageName, snapshot)
            parts += reconciliationFor(packageName, snapshot)
            parts += notificationsFor(packageName, snapshot)
            parts += usbFor(packageName, snapshot)

            if (parts.isEmpty()) return@mapNotNull null
            ScoredApp(
                packageName = packageName,
                score = parts.sumOf { it.second },
                reasons = parts.map { "${it.first} ${it.second}" },
            )
        }
    }

    private fun pullsFor(packageName: String, snapshot: ContextSnapshot) =
        ContextualRules.pulls
            .filter { it.target == packageName }
            .mapNotNull { pull ->
                val launched = snapshot.sessions.any {
                    it.packageName == pull.source && it.start.isWithin(pull.window, snapshot.now)
                }
                if (launched) "pull from ${pull.source.short()}" to pull.weight else null
            }

    /**
     * Trust's outcome, read from the most recent visit that ended inside the window.
     *
     * The visit has to have ended: while it is still open there is no telling which way it went.
     */
    private fun trustBranch(packageName: String, snapshot: ContextSnapshot): List<Pair<String, Int>> {
        val visit = snapshot.sessions
            .filter { it.packageName == ContextApps.TRUST }
            .firstOrNull { it.start.plus(it.duration).isWithin(ContextualRules.TrustWindow, snapshot.now) }
            ?: return emptyList()

        val short = visit.duration < ContextualRules.TrustShortVisit
        val table = if (short) ContextualRules.trustShortPulls else ContextualRules.trustLongPulls
        val weight = table[packageName] ?: return emptyList()

        val why = if (short) "trust visit under 2 min" else "trust payment went through"
        return listOf(why to weight)
    }

    private fun rampFor(packageName: String, snapshot: ContextSnapshot): List<Pair<String, Int>> {
        val ramp = ContextualRules.ramps.firstOrNull { it.packageName == packageName }
            ?: return emptyList()

        val last = snapshot.lastUsed[packageName]
        val days = if (last == null) Long.MAX_VALUE else Duration.between(last, snapshot.now).toDays()

        val score = when {
            days < ramp.startDay -> ramp.floor
            days >= ramp.peakDay -> ramp.peakScore
            else -> {
                val through = (days - ramp.startDay).toFloat() / (ramp.peakDay - ramp.startDay)
                (through * ramp.peakScore).toInt()
            }
        }

        return if (score == 0) emptyList() else listOf("dormant $days days" to score)
    }

    private fun reconciliationFor(
        packageName: String,
        snapshot: ContextSnapshot,
    ): List<Pair<String, Int>> {
        val boost = ContextualRules.reconciliation[packageName] ?: return emptyList()

        val today = LocalDate.ofInstant(snapshot.now, snapshot.zone)
        if (today.dayOfMonth < ContextualRules.ReconciliationDay) return emptyList()

        val since = today.withDayOfMonth(ContextualRules.ReconciliationDay)
            .atStartOfDay(snapshot.zone)
            .toInstant()

        val done = snapshot.lastUsed[packageName]?.isAfter(since) == true
        return if (done) emptyList() else listOf("statement not checked this month" to boost)
    }

    private fun notificationsFor(
        packageName: String,
        snapshot: ContextSnapshot,
    ): List<Pair<String, Int>> = when {
        packageName in snapshot.sticky ->
            listOf("something in progress" to ContextualRules.StickyNotificationBoost)

        // Only for apps a rule already names, so a chatty app cannot displace a deliberate one.
        packageName in snapshot.notified && packageName in ContextApps.all ->
            listOf("unread" to ContextualRules.RegularNotificationBoost)

        else -> emptyList()
    }

    private fun usbFor(packageName: String, snapshot: ContextSnapshot) =
        if (snapshot.usbDataConnected && packageName == ContextualRules.UsbSettingsTarget) {
            listOf("usb data connected" to ContextualRules.UsbSettingsBoost)
        } else {
            emptyList()
        }

    private fun Instant.isWithin(window: Duration, now: Instant) =
        !isAfter(now) && Duration.between(this, now) <= window

    private fun String.short() = substringAfterLast('.')
}
