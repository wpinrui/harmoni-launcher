package com.wpinrui.harmoni.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.wpinrui.harmoni.shortcuts.AppShortcut
import com.wpinrui.harmoni.shortcuts.AppShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * Picks one of the shortcuts published on the device, the ones other launchers show on a long
 * press of an icon.
 *
 * One flat list rather than app then shortcut: most apps publish none, so a list of apps would be
 * mostly dead ends.
 */
@Composable
fun ShortcutPicker(
    gestureLabel: String,
    bound: Boolean,
    onPick: (AppShortcut) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    // One binder call for the whole device, off the main thread, since it walks every package.
    // Null until it has answered, so an empty list is only reported once it means something.
    val shortcuts by produceState<List<AppShortcut>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { AppShortcuts.all(context) }
    }

    val results = remember(shortcuts, query) {
        val all = shortcuts.orEmpty()
        if (query.isBlank()) {
            all
        } else {
            val needle = query.trim().lowercase()
            all.filter { needle in it.label.lowercase() || needle in it.appLabel.lowercase() }
        }
    }

    PickerDialog(
        query = query,
        onQueryChange = { query = it },
        onDismiss = onDismiss,
        title = gestureLabel,
        actions = { if (bound) PickerAction("UNBIND", accented = true, onClick = onClear) },
    ) {
        if (shortcuts?.isEmpty() == true) {
            item {
                Text(
                    text = "No shortcuts published, or Harmoni is not the home app.",
                    style = NoteStyle,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        items(results, key = { it.packageName + "/" + it.id }) { shortcut ->
            PickerRow(
                title = shortcut.label,
                subtitle = shortcut.appLabel,
                onClick = { onPick(shortcut) },
                icon = { ShortcutIcon(shortcut) },
            )
        }
    }
}

/** Shortcut icons come from the publishing app and have to be fetched one at a time. */
@Composable
private fun ShortcutIcon(shortcut: AppShortcut) {
    val context = LocalContext.current

    val icon by produceState<ImageBitmap?>(initialValue = null, shortcut) {
        value = withContext(Dispatchers.IO) {
            runCatching { AppShortcuts.iconOf(context, shortcut)?.toBitmap()?.asImageBitmap() }
                .getOrNull()
        }
    }

    icon?.let {
        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(IconSize))
    }
}
