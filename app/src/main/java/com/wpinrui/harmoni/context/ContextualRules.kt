package com.wpinrui.harmoni.context

import com.wpinrui.harmoni.context.ContextApps.CALENDAR
import com.wpinrui.harmoni.context.ContextApps.CITIBANK
import com.wpinrui.harmoni.context.ContextApps.DBS_DIGIBANK
import com.wpinrui.harmoni.context.ContextApps.GRAB
import com.wpinrui.harmoni.context.ContextApps.KCUTS_GO
import com.wpinrui.harmoni.context.ContextApps.MAPS
import com.wpinrui.harmoni.context.ContextApps.NTU_OMNIBUS
import com.wpinrui.harmoni.context.ContextApps.OCBC
import com.wpinrui.harmoni.context.ContextApps.OUTLOOK
import com.wpinrui.harmoni.context.ContextApps.PAYLAH
import com.wpinrui.harmoni.context.ContextApps.SC_MOBILE
import com.wpinrui.harmoni.context.ContextApps.SETTINGS
import com.wpinrui.harmoni.context.ContextApps.SINGABUS
import com.wpinrui.harmoni.context.ContextApps.TRUST
import com.wpinrui.harmoni.context.ContextApps.WALLET
import com.wpinrui.harmoni.context.ContextApps.WHATSAPP
import java.time.Duration

/**
 * The rules, as data.
 *
 * Kept apart from the arithmetic that applies them so the weights can be read and argued about
 * without reading any code. Every number here came from a decision, not a default.
 *
 * Some entries are dead as things stand, because their target sits on the fixed ring or the clock
 * block and is filtered out at the end. They are kept because the exclusion is a property of
 * today's bindings, not of the rule: move an app off the fixed ring and its rules wake up.
 */
object ContextualRules {

    /** One app's launch raising another for a while, then dropping off a cliff. */
    data class Pull(
        val source: String,
        val target: String,
        val weight: Int,
        val window: Duration,
    )

    private val Transit: Duration = Duration.ofMinutes(30)
    private val Banking: Duration = Duration.ofMinutes(5)
    private val Comms: Duration = Duration.ofMinutes(20)

    val pulls = listOf(
        // Transit. Checking one way of getting somewhere raises the others, except that
        // committing to a Grab and waiting for the campus shuttle are mutually exclusive.
        Pull(MAPS, MAPS, 50, Transit),
        Pull(MAPS, NTU_OMNIBUS, 45, Transit),
        Pull(MAPS, SINGABUS, 45, Transit),
        Pull(MAPS, GRAB, 40, Transit),
        Pull(SINGABUS, SINGABUS, 50, Transit),
        Pull(SINGABUS, MAPS, 55, Transit),
        Pull(SINGABUS, GRAB, 35, Transit),
        Pull(NTU_OMNIBUS, NTU_OMNIBUS, 50, Transit),
        Pull(NTU_OMNIBUS, MAPS, 45, Transit),
        Pull(NTU_OMNIBUS, GRAB, -60, Transit),
        Pull(GRAB, GRAB, 50, Transit),
        Pull(GRAB, MAPS, 55, Transit),
        Pull(GRAB, NTU_OMNIBUS, -60, Transit),

        // Banking. The reconciliation trio pull each other; the everyday pair pull each other.
        // Trust is handled separately, since its effect depends on how long the visit lasted.
        Pull(SC_MOBILE, SC_MOBILE, 45, Banking),
        Pull(SC_MOBILE, TRUST, 50, Banking),
        Pull(SC_MOBILE, CITIBANK, 45, Banking),
        Pull(CITIBANK, CITIBANK, 45, Banking),
        Pull(CITIBANK, TRUST, 50, Banking),
        Pull(CITIBANK, SC_MOBILE, 45, Banking),
        Pull(DBS_DIGIBANK, DBS_DIGIBANK, 45, Banking),
        Pull(DBS_DIGIBANK, OCBC, 45, Banking),
        Pull(OCBC, OCBC, 45, Banking),
        Pull(OCBC, DBS_DIGIBANK, 45, Banking),

        // Comms.
        Pull(OUTLOOK, OUTLOOK, 45, Comms),
        Pull(OUTLOOK, CALENDAR, 45, Comms),
        Pull(WHATSAPP, WHATSAPP, 40, Comms),
        Pull(WHATSAPP, CALENDAR, 40, Comms),
    )

    /**
     * Trust's branch, decided when the visit ends rather than when it starts.
     *
     * A short visit means the QR was not supported, so PayLah is offered as the fallback. A long
     * one means the payment went through, so PayLah is pushed away and the two cards that need
     * reconciling against it are raised instead.
     */
    val TrustWindow: Duration = Banking
    val TrustShortVisit: Duration = Duration.ofMinutes(2)
    val trustShortPulls = mapOf(PAYLAH to 70)
    val trustLongPulls = mapOf(SC_MOBILE to 50, CITIBANK to 50, PAYLAH to -50)

    /** Baselines, before anything else is added. */
    fun baseline(packageName: String, motion: MotionState): Int = when (packageName) {
        // Paying is the default reason to reach for the phone, unless you are in a vehicle,
        // where you are plainly not at a counter.
        PAYLAH -> if (motion == MotionState.IN_VEHICLE) 10 else 55
        TRUST -> if (motion == MotionState.IN_VEHICLE) 10 else 35
        WALLET -> 20
        else -> 0
    }

    /**
     * Moving raises the ways of getting about.
     *
     * Walking is a nudge, because a phone in a pocket reads as walking for much of the day. In a
     * vehicle is unambiguous, so it counts for as much as a strong pull.
     */
    fun motionBoost(packageName: String, motion: MotionState): Int = when (motion) {
        MotionState.WALKING -> if (packageName in transitApps) 10 else 0

        MotionState.IN_VEHICLE -> when (packageName) {
            MAPS -> 45
            NTU_OMNIBUS, SINGABUS, GRAB -> 40
            else -> 0
        }

        MotionState.STILL, MotionState.UNKNOWN -> 0
    }

    private val transitApps = setOf(MAPS, NTU_OMNIBUS, SINGABUS, GRAB)

    /**
     * A dormant app climbing back into view.
     *
     * Below the start day it is actively pushed down, because it was just done and will not be
     * wanted again. After that it climbs to a peak and stays there until it is used.
     */
    data class Ramp(
        val packageName: String,
        val floor: Int,
        val startDay: Int,
        val peakDay: Int,
        val peakScore: Int,
    )

    val ramps = listOf(
        Ramp(packageName = KCUTS_GO, floor = -20, startDay = 21, peakDay = 35, peakScore = 60),
    )

    /**
     * The monthly card reconciliation, live from the tenth until all three have been opened.
     *
     * Opening one before the tenth does not count: the point is to check the month's statements
     * once they exist, not to be let off by having glanced at the app earlier.
     */
    const val ReconciliationDay = 10
    val reconciliation = mapOf(TRUST to 75, SC_MOBILE to 65, CITIBANK to 65)

    /** Plugging into a computer means Settings, and it is not a close call. */
    const val UsbSettingsBoost = 1000
    const val UsbSettingsTarget = SETTINGS

    /**
     * Notifications as evidence of intent.
     *
     * An ongoing notification means something is genuinely in progress, a ride or a download, so
     * it can promote an app no rule ever named. A regular one is only a nudge, and only for apps
     * the rules already know, otherwise a chatty app would crowd out everything deliberate.
     */
    const val StickyNotificationBoost = 80
    const val RegularNotificationBoost = 25
}
