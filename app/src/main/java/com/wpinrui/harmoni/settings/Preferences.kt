package com.wpinrui.harmoni.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * The one line every stored setting shares.
 *
 * Preferences rather than a file because these are a handful of values written one at a time at
 * human speed, which is exactly what they are for. Always the application context: these stores
 * outlive any screen that writes to them.
 *
 * A store that holds a setting calls [com.wpinrui.harmoni.app.SettingsRestart.mark] when it
 * writes, because nothing propagates to a running home surface. A store that holds a counter does
 * not, because a count changes nothing anybody is looking at.
 */
fun Context.preferences(file: String): SharedPreferences =
    applicationContext.getSharedPreferences(file, Context.MODE_PRIVATE)
