package com.wpinrui.harmoni.apps

import android.content.Context
import androidx.core.content.edit
import com.wpinrui.harmoni.app.SettingsRestart
import com.wpinrui.harmoni.settings.preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Apps Harmoni pretends are not installed.
 *
 * Hidden everywhere it surfaces an app: the search view and the all apps grid, the contextual
 * ring's candidates, and the list the fixed ring is bound from. The app is still installed and
 * still launchable by other means; Harmoni simply stops offering it.
 */
object HiddenApps {

    private const val FILE = "hidden_apps"

    private const val KEY = "packages"

    private val _packages = MutableStateFlow<Set<String>>(emptySet())
    val packages: StateFlow<Set<String>> = _packages.asStateFlow()

    fun load(context: Context) {
        _packages.value = context.preferences(FILE).getStringSet(KEY, emptySet()).orEmpty()
    }

    /** Written on every change, since the screen has no save button and is not meant to need one. */
    fun set(context: Context, packageName: String, hidden: Boolean) {
        val next = if (hidden) _packages.value + packageName else _packages.value - packageName
        context.preferences(FILE).edit { putStringSet(KEY, next) }
        _packages.value = next
        SettingsRestart.mark()
    }
}

/** Everything the launcher will admit to, which is what every surface should be filtering from. */
fun List<AppEntry>.visible(hidden: Set<String>): List<AppEntry> =
    filterNot { it.packageName in hidden }
