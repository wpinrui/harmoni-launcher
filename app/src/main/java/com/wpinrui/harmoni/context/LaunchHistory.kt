package com.wpinrui.harmoni.context

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.Duration
import java.time.Instant

/** One visit to an app: when it came to the foreground, and for how long. */
data class AppSession(
    val packageName: String,
    val start: Instant,
    val duration: Duration,
)

/**
 * What the rules know about the past, read from usage stats.
 *
 * Two different questions need two different queries. The pair pulls care about the last half
 * hour in detail, including how long a visit lasted, which only the event stream carries. The
 * ramp and the monthly gate care about "when did this app last run at all", weeks back, which
 * the event stream no longer holds but the aggregate does.
 */
class LaunchHistory(private val context: Context) {

    private val usage = context.getSystemService(UsageStatsManager::class.java)

    /** Sessions that started inside the last [RECENT_WINDOW], newest first. */
    fun recentSessions(now: Instant): List<AppSession> {
        val begin = now.minus(RECENT_WINDOW)
        val events = usage.queryEvents(begin.toEpochMilli(), now.toEpochMilli())

        val openedAt = mutableMapOf<String, Long>()
        val sessions = mutableListOf<AppSession>()
        val event = UsageEvents.Event()

        while (events.getNextEvent(event)) {
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    openedAt.putIfAbsent(event.packageName, event.timeStamp)

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = openedAt.remove(event.packageName) ?: continue
                    sessions += AppSession(
                        packageName = event.packageName,
                        start = Instant.ofEpochMilli(start),
                        duration = Duration.ofMillis(event.timeStamp - start),
                    )
                }
            }
        }

        // Anything still open ran from its resume until now, which is what a rule asking "did I
        // just use this" wants to hear.
        openedAt.forEach { (packageName, start) ->
            sessions += AppSession(
                packageName = packageName,
                start = Instant.ofEpochMilli(start),
                duration = Duration.ofMillis(now.toEpochMilli() - start),
            )
        }

        return merged(sessions).sortedByDescending { it.start }
    }

    /**
     * Joins the fragments of one visit back together.
     *
     * Moving between an app's own activities produces a PAUSED and then a RESUMED, so a single
     * visit arrives here in pieces. The rules that ask how long a visit lasted, the Trust branch
     * above all, would read a five-minute payment as several short ones and take the wrong
     * branch.
     */
    private fun merged(sessions: List<AppSession>): List<AppSession> =
        sessions.groupBy { it.packageName }.values.flatMap { forApp ->
            forApp.sortedBy { it.start }.fold(mutableListOf<AppSession>()) { joined, session ->
                val previous = joined.lastOrNull()
                val gap = previous?.let { Duration.between(it.start + it.duration, session.start) }

                if (previous != null && gap != null && gap <= SAME_VISIT_GAP) {
                    joined[joined.lastIndex] = previous.copy(
                        duration = Duration.between(previous.start, session.start + session.duration),
                    )
                } else {
                    joined += session
                }

                joined
            }
        }

    /** When each app was last used, going back [HISTORY_WINDOW]. */
    fun lastUsed(now: Instant): Map<String, Instant> {
        val begin = now.minus(HISTORY_WINDOW)
        return usage.queryAndAggregateUsageStats(begin.toEpochMilli(), now.toEpochMilli())
            .filterValues { it.lastTimeUsed > 0 }
            .mapValues { (_, stats) -> Instant.ofEpochMilli(stats.lastTimeUsed) }
    }

    companion object {
        /** Long enough to cover an activity handover, short enough not to join two visits. */
        private val SAME_VISIT_GAP: Duration = Duration.ofSeconds(5)

        /** Comfortably past the longest pull window, which is transit at 30 minutes. */
        private val RECENT_WINDOW: Duration = Duration.ofHours(2)

        /** Past the top of the Kcuts ramp at 35 days, with room to spare. */
        private val HISTORY_WINDOW: Duration = Duration.ofDays(120)
    }
}

/**
 * Whether usage access has been granted by hand in Settings.
 *
 * Without it every query returns empty rather than failing, so the contextual ring would quietly
 * fall back to baselines alone. Worth checking so that state can be reported rather than guessed.
 */
fun Context.hasUsageAccess(): Boolean {
    val appOps = getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        packageName,
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
