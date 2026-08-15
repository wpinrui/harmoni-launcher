package com.wpinrui.harmoni.home

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.util.Log
import androidx.annotation.DrawableRes
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.context.ContextApps
import com.wpinrui.harmoni.R

/**
 * What the home block points at.
 *
 * Named here rather than configured, per the design document, and deliberately independent of the
 * ring's eight.
 */
object HomeBindings {
    const val TELEGRAM = "org.telegram.messenger"
    const val INSTAGRAM = "com.instagram.android"
    const val WHATSAPP = ContextApps.WHATSAPP
    const val YOUTUBE = "com.google.android.youtube"
    const val YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"

    /** In the order they sit on the block. */
    val badged = listOf(
        Badge(TELEGRAM, R.drawable.badge_telegram),
        Badge(INSTAGRAM, R.drawable.badge_instagram),
        Badge(WHATSAPP, R.drawable.badge_whatsapp),
    )
}

/**
 * A badged app and the icon it wears on the block.
 *
 * The badges use drawn icons rather than the installed app's own, so the three read as one set at
 * 24dp. Attribution for them is in `README.md` and on the launcher app screen.
 */
data class Badge(val packageName: String, @param:DrawableRes val icon: Int)

/** Opens an app by package name. Does nothing if it is not installed. */
fun Context.launchApp(packageName: String) {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    if (intent == null) {
        Log.w("HomeBindings", "No launch intent for $packageName; is it installed?")
        return
    }
    startActivity(intent)
}

/**
 * Launches an indexed entry, in the profile it belongs to.
 *
 * [Context.launchApp] resolves only within the calling profile, so a work-profile app would be
 * listed, would draw its icon, and would then do nothing when tapped. The index enumerates
 * profiles deliberately and [AppEntry.user] is part of the identity, so the launch has to carry it.
 */
fun Context.launch(entry: AppEntry) {
    val launcherApps = getSystemService(LauncherApps::class.java)

    runCatching { launcherApps.startMainActivity(entry.component, entry.user, null, null) }
        .onFailure { Log.w("HomeBindings", "Cannot launch ${entry.component}", it) }
}

/**
 * Starts [intent], and does nothing louder than a log if nothing can handle it.
 *
 * The clock block's targets are all standard actions, but which app answers them is the device's
 * business, and on a device where none does, a tap on the time should be a tap that did nothing
 * rather than a crash on the home screen.
 */
fun Context.launchOrLog(intent: Intent, what: String) {
    runCatching { startActivity(intent) }
        .onFailure { Log.w("HomeBindings", "Nothing handles $what", it) }
}
