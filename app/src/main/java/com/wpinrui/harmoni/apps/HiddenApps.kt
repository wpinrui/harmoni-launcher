package com.wpinrui.harmoni.apps

import android.content.Context
import androidx.core.content.edit
import com.wpinrui.harmoni.settings.PreferenceStore
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
private const val FILE = "hidden_apps"

object HiddenApps : PreferenceStore(FILE) {

    private const val KEY = "packages"

    private val _packages = MutableStateFlow<Set<String>>(emptySet())
    val packages: StateFlow<Set<String>> = _packages.asStateFlow()

    override fun load(context: Context) {
        _packages.value = preferences(context).getStringSet(KEY, emptySet()).orEmpty()
    }

    /** Written on every change, since the screen has no save button and is not meant to need one. */
    fun set(context: Context, packageName: String, hidden: Boolean) {
        val next = if (hidden) _packages.value + packageName else _packages.value - packageName
        preferences(context).edit { putStringSet(KEY, next) }
        _packages.value = next
        settingChanged()
    }
}

/** Everything the launcher will admit to, which is what every surface should be filtering from. */
fun List<AppEntry>.visible(hidden: Set<String>): List<AppEntry> =
    filterNot { it.packageName in hidden }
