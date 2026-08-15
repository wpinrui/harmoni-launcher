package com.wpinrui.harmoni.home

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
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
    const val WHATSAPP = "com.whatsapp"
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
 * 24dp. Attribution for them is in `README.md` and belongs on the launcher app screen.
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
