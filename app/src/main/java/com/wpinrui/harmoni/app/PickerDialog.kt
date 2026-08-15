package com.wpinrui.harmoni.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.wpinrui.harmoni.ui.theme.Accent
import com.wpinrui.harmoni.ui.theme.noRipple

/**
 * A searchable list in a card, which is what both of this screen's pickers are.
 *
 * Full width rather than the platform default, because the lists are every installed app and every
 * shortcut on the device, and a dialog-width column of those is a keyhole.
 *
 * The card is sized to what is actually visible rather than to a fraction of the screen, so it
 * shrinks as the keyboard rises instead of sliding its buttons underneath it.
 */
@Composable
fun PickerDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    rows: LazyListScope.() -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Without this the dialog's window is laid out as though the keyboard were not there,
            // so the insets below never report it and the buttons end up underneath it.
            decorFitsSystemWindows = false,
        ),
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
                title?.let {
                    Text(
                        text = it.uppercase(),
                        style = HeaderStyle,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                    )
                }

                SearchField(query = query, onQueryChange = onQueryChange, focus = focus)

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    content = rows,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.10f)),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    actions()
                    PickerAction("CANCEL", onClick = onDismiss)
                }
            }
        }
    }
}

/** One row: an icon, a name, and what it belongs to underneath. */
@Composable
fun PickerRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(IconSize), contentAlignment = Alignment.Center) { icon() }

        Column {
            Text(text = title, style = ValueStyle)
            Text(text = subtitle, style = NoteStyle)
        }
    }
}

@Composable
fun PickerAction(label: String, accented: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .noRipple(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        style = if (accented) ValueStyle.copy(color = Accent) else ValueStyle,
    )
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

internal val IconSize = 30.dp
