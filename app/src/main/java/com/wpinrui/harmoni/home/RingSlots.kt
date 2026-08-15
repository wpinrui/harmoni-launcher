package com.wpinrui.harmoni.home

import android.content.Context
import androidx.core.content.edit
import com.wpinrui.harmoni.app.SettingsRestart
import com.wpinrui.harmoni.settings.preferences
import com.wpinrui.harmoni.apps.AppEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What is actually on the fixed ring, which is [RingBindings] plus whatever has been swapped.
 *
 * The compiled bindings stay the defaults rather than being copied into storage at first run, so a
 * slot that was never touched still follows the source, and resetting one is a delete rather than
 * a restore.
 */
object RingSlots {

    private const val FILE = "ring"

    private const val SLOT = "slot_"
    private const val SHOW_IN_SEARCH = "show_in_search"

    private val _overrides = MutableStateFlow<Map<Int, String>>(emptyMap())

    /** Position to package, for the positions that have been changed. */
    val overrides: StateFlow<Map<Int, String>> = _overrides.asStateFlow()

    private val _showInSearch = MutableStateFlow(false)

    /**
     * Whether the ring's apps also appear in search.
     *
     * Off by default: an app that is one tap away on the ring does not need to take a place in a
     * grid of eight, and eight of the most-used apps crowding the first page is most of that page.
     */
    val showInSearch: StateFlow<Boolean> = _showInSearch.asStateFlow()

    fun load(context: Context) {
        val preferences = context.preferences(FILE)

        _overrides.value = preferences.all
            .filterKeys { it.startsWith(SLOT) }
            .mapNotNull { (key, value) ->
                val index = key.removePrefix(SLOT).toIntOrNull() ?: return@mapNotNull null
                val packageName = value as? String ?: return@mapNotNull null
                index to packageName
            }
            .toMap()

        _showInSearch.value = preferences.getBoolean(SHOW_IN_SEARCH, false)
    }

    fun bind(context: Context, index: Int, packageName: String) {
        context.preferences(FILE).edit { putString("$SLOT$index", packageName) }
        _overrides.value = _overrides.value + (index to packageName)
        SettingsRestart.mark()
    }

    fun reset(context: Context, index: Int) {
        context.preferences(FILE).edit { remove("$SLOT$index") }
        _overrides.value = _overrides.value - index
        SettingsRestart.mark()
    }

    /** Hiding an app it is bound to takes it off the ring, back to whatever the source says. */
    fun resetBindingsTo(context: Context, packageName: String) {
        _overrides.value.filterValues { it == packageName }.keys.forEach { reset(context, it) }
    }

    fun setShowInSearch(context: Context, show: Boolean) {
        context.preferences(FILE).edit { putBoolean(SHOW_IN_SEARCH, show) }
        _showInSearch.value = show
        SettingsRestart.mark()
    }
}

/**
 * The eight, with any swapped position resolved against what is installed.
 *
 * Labels come from the index rather than being stored beside the package, so renaming or updating
 * an app changes what the ring calls it without anything having to be rebound.
 *
 * A position holding a hidden app comes back null and is left blank. The eight are positional, so
 * closing the gap would move every app after it and cost the muscle memory the ring is for.
 */
fun ringTargets(
    overrides: Map<Int, String>,
    entries: List<AppEntry>,
    hidden: Set<String> = emptySet(),
): List<RingTarget?> =
    RingBindings.slots.mapIndexed { index, default ->
        val target = overrides[index]?.let { packageName ->
            val label = entries.firstOrNull { it.packageName == packageName }?.label ?: packageName
            RingTarget.App(packageName, label)
        } ?: default

        // Tested against the app a slot launches, not the package its icon comes from: a pinned
        // web app borrows its browser's icon, and hiding the browser should not take the
        // bookmark with it.
        if ((target as? RingTarget.App)?.packageName in hidden) null else target
    }
