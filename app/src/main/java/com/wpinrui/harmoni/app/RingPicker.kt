package com.wpinrui.harmoni.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.search.AppMatcher

/**
 * Picks the app for one ring position.
 *
 * Full screen over the launcher app screen rather than a dialog: the list is every installed app,
 * and a dialog would be a small window onto a long list.
 *
 * The keyboard is welcome here. This is an ordinary app screen rather than the home surface, so
 * there is no Graffiti to stand aside for.
 */
@Composable
fun RingPicker(
    position: String,
    current: String,
    entries: List<AppEntry>,
    swapped: Boolean,
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

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .padding(horizontal = 28.dp),
    ) {
        Text(
            text = "$position, NOW ${current.uppercase()}",
            style = TitleStyle,
            modifier = Modifier.padding(top = 64.dp, bottom = 10.dp),
        )

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                singleLine = true,
                textStyle = RowStyle.copy(fontSize = 26.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                cursorBrush = SolidColor(Accent),
            )
            if (query.isEmpty()) {
                Text(text = "Search", style = RowStyle.copy(fontSize = 26.sp, color = Muted))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 104.dp),
        ) {
            if (swapped) {
                item { Row2("Put the original back", "RESET", Accent, onReset) }
            }

            items(results, key = { it.component.flattenToShortString() + it.user }) { entry ->
                Row2(entry.label, entry.packageName, Muted) { onPick(entry) }
            }
        }
    }
}
