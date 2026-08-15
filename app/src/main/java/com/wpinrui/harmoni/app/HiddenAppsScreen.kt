package com.wpinrui.harmoni.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.apps.AppEntryIcon
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.home.RingSlots

/**
 * Which apps Harmoni pretends are not installed.
 *
 * Checked means hidden. Saved on the tap: there is no save button and nothing to confirm, and the
 * restart that carries the change through happens on the way out of this screen.
 */
@Composable
fun HiddenAppsScreen() {
    val context = LocalContext.current
    val entries by context.harmoni.appIndex.entries.collectAsState()
    val hidden by HiddenApps.packages.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(entries, key = { it.component.flattenToShortString() + it.user }) { entry ->
            HiddenRow(
                entry = entry,
                hidden = entry.packageName in hidden,
                onToggle = { hide ->
                    HiddenApps.set(context, entry.packageName, hide)
                    // A hidden app cannot stay bound to a ring position, so the slot goes back to
                    // whatever the source says it is.
                    if (hide) RingSlots.resetBindingsTo(context, entry.packageName)
                },
            )
        }
    }
}

@Composable
private fun HiddenRow(entry: AppEntry, hidden: Boolean, onToggle: (Boolean) -> Unit) {
    Panel(onClick = { onToggle(!hidden) }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppEntryIcon(entry = entry, size = 30.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.label, style = ValueStyle)
                Text(text = entry.packageName, style = NoteStyle)
            }

            Checkbox(
                checked = hidden,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = Accent,
                    checkmarkColor = Background,
                    uncheckedColor = Color.White.copy(alpha = 0.30f),
                ),
            )
        }
    }
}
