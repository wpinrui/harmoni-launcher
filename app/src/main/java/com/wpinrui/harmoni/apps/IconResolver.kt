package com.wpinrui.harmoni.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable

/**
 * Where every surface gets an app icon.
 *
 * Section 2 wants an icon-pack drawable where one exists and the app's own icon otherwise. Only
 * the fallback half exists today, because no pack has been chosen yet. Pack lookup will land
 * behind this interface, so the ring and the search grid never learn which half answered.
 */
interface IconResolver {
    fun iconFor(entry: AppEntry): Drawable?
}

/** The app's own icon, rendered for the launcher's density. */
class SystemIconResolver(context: Context) : IconResolver {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val densityDpi = context.resources.displayMetrics.densityDpi

    override fun iconFor(entry: AppEntry): Drawable? {
        val intent = Intent(Intent.ACTION_MAIN).setComponent(entry.component)
        return launcherApps.resolveActivity(intent, entry.user)?.getIcon(densityDpi)
    }
}
