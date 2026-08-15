package com.wpinrui.harmoni.search

import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.apps.AppEntryIcon
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.ui.theme.Karla
import kotlin.math.ceil

/**
 * The search view from Section 4, over the home surface.
 *
 * A double tap opens it with nothing typed, which makes it the all apps screen: the query is
 * simply empty and every installed app matches. Typing narrows it.
 *
 * The soft keyboard stands in for Graffiti until the alphabet exists. It is the only part of this
 * screen the design does not call for, and it comes out again when strokes go in.
 *
 * It lives in its own window rather than in the launcher's. That is what makes the backdrop blur
 * possible: the system can only blur what is *behind* a window, and the launcher's own window is
 * where the wallpaper and the clock block are drawn, so asking it to blur itself does nothing.
 */
@Composable
fun SearchSurface(onLaunch: (AppEntry) -> Unit, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        SearchWindow(onLaunch = onLaunch, onClose = onClose)
    }
}

@Composable
private fun SearchWindow(onLaunch: (AppEntry) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val entries by context.harmoni.appIndex.entries.collectAsState()
    var query by remember { mutableStateOf("") }

    val results = remember(entries, query) { AppMatcher.match(entries, query) }
    val pageCount = maxOf(1, ceil(results.size / PerPage.toFloat()).toInt())
    val pager = rememberPagerState(pageCount = { pageCount })

    // A narrower query is a different set of results, so start again from the first page.
    LaunchedEffect(query) { pager.scrollToPage(0) }

    // Everything arrives together: the blur deepens as the tint and the grid fade up, rather
    // than the panel appearing over a sharp wallpaper and catching up.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "search-entrance",
    )

    ConfigureWindow()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(entrance)
            // Tint over blur, as the design has it: the blur softens the wallpaper, the tint
            // darkens it enough for white text to sit on.
            .background(Tint)
            // Anywhere that is not the grid or the query closes the view. The lower half is
            // where Graffiti will live, so this is a stand-in for a stroke area, not a design.
            .noRipple(onClose),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            QueryField(query = query, onQueryChange = { query = it })

            ResultSummary(count = results.size, hasQuery = query.isNotEmpty())

            HorizontalPager(
                state = pager,
                modifier = Modifier.padding(top = 30.dp),
            ) { page ->
                AppPage(
                    apps = results.drop(page * PerPage).take(PerPage),
                    onLaunch = onLaunch,
                )
            }

            PageDots(pageCount = pageCount, current = pager.currentPage)

            GraffitiSpace()
        }
    }
}

@Composable
private fun QueryField(query: String, onQueryChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    Box(
        modifier = Modifier
            .padding(start = 30.dp, end = 30.dp, top = 72.dp)
            .heightIn(min = 46.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = { onQueryChange(it.lowercase()) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.ExtraLight,
                fontSize = 38.sp,
                letterSpacing = 0.04.em,
                color = Color.White,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search,
            ),
            cursorBrush = SolidColor(Color.White),
        )
    }
}

@Composable
private fun ResultSummary(count: Int, hasQuery: Boolean) {
    Row(
        modifier = Modifier.padding(start = 30.dp, end = 30.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (count == 1) "1 RESULT" else "$count RESULTS",
            style = MetaStyle,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.24f)),
        )
        // Nothing to erase until something is typed.
        if (hasQuery) Text(text = "SWIPE LEFT TO ERASE", style = MetaStyle)
    }
}

@Composable
private fun AppPage(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        apps.chunked(Columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { entry ->
                    AppCell(entry = entry, onLaunch = onLaunch, modifier = Modifier.weight(1f))
                }
                // Keeps a short last row aligned with the one above it.
                repeat(Columns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AppCell(entry: AppEntry, onLaunch: (AppEntry) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.noRipple { onLaunch(entry) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        AppEntryIcon(entry = entry, size = 52.dp, modifier = Modifier.clip(IconShape))
        Text(
            text = entry.label,
            modifier = Modifier.widthIn(max = 74.dp),
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.Light,
                fontSize = 10.5.sp,
                lineHeight = 1.25.em,
                letterSpacing = 0.05.em,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun PageDots(pageCount: Int, current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (page == current) 1f else 0.28f)),
            )
        }
    }
}

/** Held open for Section 4's strokes. Empty for now, and labelled so it reads as reserved. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.GraffitiSpace() {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Text(
            text = "GRAFFITI INPUT",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 76.dp),
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 0.24.em,
                color = Color.White.copy(alpha = 0.32f),
            ),
        )
    }
}

@Composable
private fun Modifier.noRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

private val Tint = Color(0xFF120E0C).copy(alpha = 0.72f)
private val IconShape = androidx.compose.foundation.shape.RoundedCornerShape(26)
private val MetaStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.12.em,
    color = Color(0xFFCFC6BD),
)

private const val Columns = 4
private const val PerPage = 8

/**
 * Sets the search view's own window up: blurred backdrop, no status bar, nothing of its own drawn.
 *
 * The blur is set once. Writing to a window's attributes forces a relayout, so doing it per frame
 * to animate the radius stalls the window until the animation ends, which reads as a jump rather
 * than a fade. The content's alpha carries the entrance instead.
 */
@Composable
private fun ConfigureWindow() {
    val view = LocalView.current
    val radius = with(LocalDensity.current) { BlurRadius.roundToPx() }
    val window = (view.parent as? DialogWindowProvider)?.window

    DisposableEffect(window) {
        if (window == null) return@DisposableEffect onDispose { }

        window.setLayout(MATCH_PARENT, MATCH_PARENT)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        // A dialog dims what is behind it by default, which over a blurred wallpaper reads as a
        // black sheet. The blur is the whole effect here.
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply {
            blurBehindRadius = radius
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        // The home surface hides the status bar; a new window does not inherit that, and would
        // otherwise leave a black band across the top of the blur.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, view).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND) }
    }
}

private val BlurRadius = 40.dp
