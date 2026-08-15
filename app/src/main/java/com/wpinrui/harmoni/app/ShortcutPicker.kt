package com.wpinrui.harmoni.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.wpinrui.harmoni.shortcuts.AppShortcut
import com.wpinrui.harmoni.shortcuts.AppShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val shortcuts by produceState(initialValue = emptyList<AppShortcut>(), context) {
        value = withContext(Dispatchers.IO) { AppShortcuts.all(context) }
    }

    val results = remember(shortcuts, query) {
        if (query.isBlank()) {
            shortcuts
        } else {
            val needle = query.trim().lowercase()
            shortcuts.filter {
                needle in it.label.lowercase() || needle in it.appLabel.lowercase()
            }
        }
    }

    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PanelColour),
            ) {
                Text(
                    text = gestureLabel.uppercase(),
                    style = HeaderStyle,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 48.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        singleLine = true,
                        textStyle = ValueStyle,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Search,
                        ),
                        cursorBrush = SolidColor(Accent),
                    )
                    if (query.isEmpty()) Text(text = "Search", style = NoteStyle)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    if (shortcuts.isEmpty()) {
                        item {
                            Text(
                                text = "No shortcuts published, or Harmoni is not the home app.",
                                style = NoteStyle,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    items(results, key = { it.packageName + "/" + it.id }) { shortcut ->
                        ShortcutRow(shortcut = shortcut, onClick = { onPick(shortcut) })
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (bound) {
                        Text(
                            text = "UNBIND",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .noRipple(onClear)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            style = ValueStyle.copy(color = Accent),
                        )
                    }
                    Text(
                        text = "CANCEL",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .noRipple(onDismiss)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        style = ValueStyle,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(shortcut: AppShortcut, onClick: () -> Unit) {
    val context = LocalContext.current

    // Shortcut icons come from the publishing app and have to be fetched one at a time.
    val icon by produceState(initialValue = null as androidx.compose.ui.graphics.ImageBitmap?, shortcut) {
        value = withContext(Dispatchers.IO) {
            runCatching { AppShortcuts.iconOf(context, shortcut)?.toBitmap()?.asImageBitmap() }
                .getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            icon?.let {
                Image(bitmap = it, contentDescription = null, modifier = Modifier.size(30.dp))
            }
        }

        Column {
            Text(text = shortcut.label, style = ValueStyle)
            Text(text = shortcut.appLabel, style = NoteStyle)
        }
    }
}
