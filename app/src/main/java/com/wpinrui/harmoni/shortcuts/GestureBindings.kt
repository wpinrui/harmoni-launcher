package com.wpinrui.harmoni.shortcuts

import android.content.Context
import androidx.core.content.edit
import com.wpinrui.harmoni.settings.PreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A gesture that can carry an app shortcut. */
enum class ShortcutGesture(val label: String) {
    SWIPE_UP("Swipe up"),
    TWO_FINGER_SWIPE_UP("Two finger swipe up"),
}

/**
 * What a shortcut binding remembers.
 *
 * The labels are stored alongside the identity so the launcher app screen can name a binding
 * without querying every shortcut on the device to do it. They are what the shortcut was called
 * when it was bound, and the binding still resolves by [id] if the app renames it.
 */
data class BoundShortcut(
    val packageName: String,
    val id: String,
    val label: String,
    val appLabel: String,
)

/** Shortcuts bound to gestures, per Harmoni's own settings rather than the GDD's fixed set. */
private const val FILE = "gesture_bindings"

object GestureBindings : PreferenceStore(FILE) {

    private val _bindings = MutableStateFlow<Map<ShortcutGesture, BoundShortcut>>(emptyMap())
    val bindings: StateFlow<Map<ShortcutGesture, BoundShortcut>> = _bindings.asStateFlow()

    // A field per key rather than one packed string: a shortcut's label is the app's own text and
    // can contain anything, including whatever separator seemed safe.
    private fun key(gesture: ShortcutGesture, field: String) = "${gesture.name}_$field"

    override fun load(context: Context) {
        val preferences = preferences(context)

        _bindings.value = ShortcutGesture.entries.mapNotNull { gesture ->
            val packageName = preferences.getString(key(gesture, "package"), null)
                ?: return@mapNotNull null
            val id = preferences.getString(key(gesture, "id"), null) ?: return@mapNotNull null

            gesture to BoundShortcut(
                packageName = packageName,
                id = id,
                label = preferences.getString(key(gesture, "label"), null).orEmpty(),
                appLabel = preferences.getString(key(gesture, "app"), null).orEmpty(),
            )
        }.toMap()
    }

    fun bind(context: Context, gesture: ShortcutGesture, shortcut: AppShortcut) {
        preferences(context).edit {
            putString(key(gesture, "package"), shortcut.packageName)
            putString(key(gesture, "id"), shortcut.id)
            putString(key(gesture, "label"), shortcut.label)
            putString(key(gesture, "app"), shortcut.appLabel)
        }

        _bindings.value = _bindings.value + (
            gesture to BoundShortcut(
                packageName = shortcut.packageName,
                id = shortcut.id,
                label = shortcut.label,
                appLabel = shortcut.appLabel,
            )
            )
        settingChanged()
    }

    fun clear(context: Context, gesture: ShortcutGesture) {
        preferences(context).edit {
            listOf("package", "id", "label", "app").forEach { remove(key(gesture, it)) }
        }
        _bindings.value = _bindings.value - gesture
        settingChanged()
    }

    fun start(context: Context, gesture: ShortcutGesture) {
        val bound = _bindings.value[gesture] ?: return
        AppShortcuts.start(context, bound.packageName, bound.id)
    }
}
