package com.wpinrui.harmoni.context

import android.content.Context
import android.util.Log
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.home.HomeBindings
import com.wpinrui.harmoni.home.RingBindings
import com.wpinrui.harmoni.home.RingTarget
import com.wpinrui.harmoni.system.NotificationCounts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Builds the contextual ring on demand.
 *
 * Almost everything is read at the moment of the long press, because the answer is only ever
 * wanted once and a stale snapshot would be worse than a slightly slower one.
 *
 * The exception is [LaunchHistory.lastUsed], which aggregates four months of usage stats and is
 * far too slow to run with a finger already down. It is refreshed in the background instead, and
 * the press reads whatever the last refresh produced. Months of history do not turn over in the
 * seconds between a refresh and a press.
 */
class ContextualRing(private val context: Context) {

    private val history = LaunchHistory(context)

    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var lastUsed: Map<String, Instant> = emptyMap()

    init {
        refresh()
    }

    /** Called when the home surface comes to the front, which is the moment before any press. */
    fun refresh() {
        scope.launch {
            lastUsed = history.lastUsed(Instant.now())
            if (lastUsed.isEmpty()) {
                Log.w(TAG, "No usage history; grant usage access or the ring is baselines only")
            }
        }
    }

    fun slots(): List<RingTarget> {
        val now = Instant.now()
        val installed = context.harmoni.appIndex.entries.value

        val snapshot = ContextSnapshot(
            now = now,
            zone = ZoneId.systemDefault(),
            motion = MotionMonitor.state.value,
            usbDataConnected = context.harmoni.usb.dataConnected,
            // Cheap: one query over the last two hours, comfortably past the longest pull
            // window.
            sessions = history.recentSessions(now),
            lastUsed = lastUsed,
            notified = NotificationCounts.counts.value.keys,
            sticky = NotificationCounts.sticky.value,
            // Hidden apps are excluded here as everywhere else: a rule can name one, but nothing
            // the launcher offers may. Harmoni itself is excluded because it is the foreground
            // app at the moment of the press, so usage stats always rank it first.
            excluded = alreadyReachable + HiddenApps.packages.value + context.packageName,
            launchable = installed.mapTo(mutableSetOf()) { it.packageName },
        )

        // Worth logging in full: a ring nobody can explain is a ring nobody trusts.
        ContextualScoring.score(snapshot)
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(12)
            .forEach { Log.d(TAG, "${it.score} ${it.packageName} ${it.reasons}") }

        // Labelled from the index rather than from the package name, which would put "harmoni" or
        // "dbsmbanking" under an icon.
        val labels = installed.associate { it.packageName to it.label }
        return ContextualScoring.ring(snapshot).map { packageName ->
            RingTarget.App(packageName, labels[packageName] ?: packageName.substringAfterLast('.'))
        }
    }

    companion object {
        /**
         * Anything already one gesture from the home surface.
         *
         * The contextual ring exists to reach what the fixed ring and the clock block cannot, so
         * spending a slot on something that is already there wastes it.
         */
        private val alreadyReachable: Set<String> =
            RingBindings.slots.map { target ->
                when (target) {
                    is RingTarget.App -> target.packageName
                    is RingTarget.Web -> target.browserPackage
                }
            }.toSet() +
                HomeBindings.badged.map { it.packageName } +
                setOf(HomeBindings.YOUTUBE, HomeBindings.YOUTUBE_MUSIC)

        private const val TAG = "ContextualRing"
    }
}
