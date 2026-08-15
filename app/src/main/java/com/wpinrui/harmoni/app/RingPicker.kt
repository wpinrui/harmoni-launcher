package com.wpinrui.harmoni.app

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            // The platform width is narrow and fixed, and this is a list of every installed app.
            usePlatformDefaultWidth = false,
            // Without this the dialog's window is laid out as though the keyboard were not there,
            // so the insets below never report it and the buttons end up underneath it.
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Bars, cutout and keyboard together, so the card is sized to what is actually
                // visible and shrinks as the keyboard rises rather than sliding under it.
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
            SearchField(query = query, onQueryChange = { query = it }, focus = focus)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                items(results, key = { it.component.flattenToShortString() + it.user }) { entry ->
                    AppRow(entry = entry, onClick = { onPick(entry) })
                }
            }

            Actions(onReset = onReset, onDismiss = onDismiss)
        }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, focus: FocusRequester) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
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
}

@Composable
private fun AppRow(entry: AppEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppEntryIcon(entry = entry, size = 30.dp)
        Column {
            Text(text = entry.label, style = ValueStyle)
            Text(text = entry.packageName, style = NoteStyle)
        }
    }
}

@Composable
private fun Actions(onReset: () -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = "RESET",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .noRipple(onReset)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            style = ValueStyle.copy(color = Accent),
        )
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
