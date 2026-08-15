package com.wpinrui.harmoni.settings

import android.content.Context
import android.content.SharedPreferences
import com.wpinrui.harmoni.app.SettingsRestart

/**
 * The shape every stored setting shares: one preferences file, read once at process start, held
 * in memory, and written on each change.
 *
 * Preferences rather than a file because these are a handful of values written one at a time at
 * human speed, which is exactly what they are for.
 *
 * [settingChanged] is what separates a setting from a counter. A setting has to reach surfaces
 * that were built from it, and nothing propagates to a running home surface, so writing one marks
 * Harmoni for a restart. A counter changes nothing anybody is looking at, so it overrides this to
 * do nothing.
 */
abstract class PreferenceStore(private val file: String) {

    protected fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(file, Context.MODE_PRIVATE)

    protected open fun settingChanged() = SettingsRestart.mark()

    /** Reads the stored state into memory. Called once, at process start. */
    abstract fun load(context: Context)
}
