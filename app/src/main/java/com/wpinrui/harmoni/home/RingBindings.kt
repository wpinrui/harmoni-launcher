package com.wpinrui.harmoni.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import com.wpinrui.harmoni.R
import com.wpinrui.harmoni.context.ContextApps

/**
 * What sits on a ring position.
 *
 * Most slots are an installed app, but a slot can also be a web address opened in a named browser,
 * which is how a pinned progressive web app reaches the ring: it has no package of its own.
 */
sealed interface RingTarget {

    val label: String

    data class App(val packageName: String, override val label: String) : RingTarget

    data class Web(
        val url: String,
        val browserPackage: String,
        override val label: String,
        /** From the repo, since a web app's own icon lives in a manifest Harmoni cannot read. */
        @param:DrawableRes val icon: Int?,
    ) : RingTarget
}

/**
 * The ring's eight, fixed in source and in fixed positions, per Section 2.
 *
 * Order is clockwise from the top: N, NE, E, SE, S, SW, W, NW. The order is the binding, so
 * moving an entry moves the app, and muscle memory is the whole point of the ring.
 */
object RingBindings {

    val slots: List<RingTarget> = listOf(
        RingTarget.App("org.mozilla.firefox", "Firefox"),
        RingTarget.Web(
            url = "https://nicer-tt.vercel.app/",
            browserPackage = "org.mozilla.firefox",
            label = "NIcEr",
            // Taken from the site's own `public/schedule.png`, since the web manifest it
            // normally comes from is not something the launcher can read.
            icon = R.drawable.ring_nicer,
        ),
        RingTarget.App("com.anthropic.claude", "Claude"),
        RingTarget.App("com.google.android.GoogleCamera", "Camera"),
        RingTarget.App("com.google.android.apps.photos", "Gallery"),
        RingTarget.App(ContextApps.CALENDAR, "Calendar"),
        RingTarget.App("sg.ndi.sp", "Singpass"),
        RingTarget.App("com.google.android.keep", "Keep"),
    )
}

/** Opens a ring slot. Does nothing if the app behind it is gone. */
fun Context.launch(target: RingTarget) {
    when (target) {
        is RingTarget.App -> launchApp(target.packageName)

        is RingTarget.Web -> {
            val intent = Intent(Intent.ACTION_VIEW, target.url.toUri())
                .setPackage(target.browserPackage)
            if (intent.resolveActivity(packageManager) == null) {
                Log.w(TAG, "${target.browserPackage} cannot open ${target.url}")
                return
            }
            startActivity(intent)
        }
    }
}

/** The package whose icon stands in for a slot. */
val RingTarget.iconPackage: String
    get() = when (this) {
        is RingTarget.App -> packageName
        is RingTarget.Web -> browserPackage
    }

private const val TAG = "RingBindings"
