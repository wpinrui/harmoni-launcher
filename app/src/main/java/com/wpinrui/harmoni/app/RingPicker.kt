package com.wpinrui.harmoni.app

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.apps.AppEntryIcon
import com.wpinrui.harmoni.search.AppMatcher

/**
 * Picks the app for one ring position.
 *
 * The keyboard is welcome here. This is an ordinary app screen, not the home surface, so there is
 * no Graffiti for it to stand aside for.
 */
@Composable
fun RingPicker(
    entries: List<AppEntry>,
    onPick: (AppEntry) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(entries, query) { AppMatcher.match(entries, query) }

    PickerDialog(
        query = query,
        onQueryChange = { query = it },
        onDismiss = onDismiss,
        actions = { PickerAction("RESET", accented = true, onClick = onReset) },
    ) {
        items(results, key = { it.component.flattenToShortString() + it.user }) { entry ->
            PickerRow(
                title = entry.label,
                subtitle = entry.packageName,
                onClick = { onPick(entry) },
                icon = { AppEntryIcon(entry = entry, size = IconSize, modifier = Modifier) },
            )
        }
    }
}
