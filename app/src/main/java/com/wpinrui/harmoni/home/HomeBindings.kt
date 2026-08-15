package com.wpinrui.harmoni.home

import android.content.Context
import android.util.Log

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
    val badged = listOf(TELEGRAM, INSTAGRAM, WHATSAPP)
}

/** Opens an app by package name. Does nothing if it is not installed. */
fun Context.launchApp(packageName: String) {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    if (intent == null) {
        Log.w("HomeBindings", "No launch intent for $packageName; is it installed?")
        return
    }
    startActivity(intent)
}
