package com.wpinrui.harmoni.context

import android.content.Context
import android.util.Log
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.home.HomeBindings
import com.wpinrui.harmoni.home.RingBindings
import com.wpinrui.harmoni.home.RingTarget
import com.wpinrui.harmoni.system.NotificationCounts
import java.time.Instant
import java.time.ZoneId

/**
 * Builds the contextual ring on demand.
 *
 * Everything is read at the moment of the long press rather than kept warm, because the answer is
 * only ever wanted once, and a stale snapshot would be worse than a slightly slower one.
 */
class ContextualRing(private val context: Context) {

    private val history = LaunchHistory(context)

    fun slots(): List<RingTarget> {
        val now = Instant.now()

        val snapshot = ContextSnapshot(
            now = now,
            zone = ZoneId.systemDefault(),
            motion = MotionMonitor.state.value,
            usbDataConnected = context.harmoni.usb.dataConnected,
            sessions = history.recentSessions(now),
            lastUsed = history.lastUsed(now),
            notified = NotificationCounts.counts.value.keys,
            sticky = NotificationCounts.sticky.value,
            excluded = alreadyReachable,
        )

        if (snapshot.lastUsed.isEmpty()) {
            Log.w(TAG, "No usage history; grant usage access or the ring is baselines only")
        }

        // Worth logging in full: a ring nobody can explain is a ring nobody trusts.
        ContextualScoring.score(snapshot)
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(12)
            .forEach { Log.d(TAG, "${it.score} ${it.packageName} ${it.reasons}") }

        return ContextualScoring.ring(snapshot).map { RingTarget.App(it, it.substringAfterLast('.')) }
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
