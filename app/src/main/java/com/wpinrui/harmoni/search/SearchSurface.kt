package com.wpinrui.harmoni.search

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.apps.AppEntryIcon
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.apps.visible
import com.wpinrui.harmoni.diagnostics.Diagnostics
import com.wpinrui.harmoni.graffiti.captureStroke
import com.wpinrui.harmoni.graffiti.isBackspaceStroke
import com.wpinrui.harmoni.graffiti.strokeSpan
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.home.RingSlots
import com.wpinrui.harmoni.home.iconPackage
import com.wpinrui.harmoni.home.ringTargets
import com.wpinrui.harmoni.ui.theme.Karla
import com.wpinrui.harmoni.ui.theme.noRipple
import kotlin.math.ceil
import kotlin.math.hypot

/**
 * The search view from Section 4, over the home surface.
 *
 * Opens on the first stroke, carrying whatever letter that stroke was, or on a double tap with
 * nothing typed at all, which makes it the all apps screen: the query is empty and every installed
 * app matches.
 *
 * Everything after that is drawn in the input area below the grid. There is no keyboard.
 */
@Composable
fun SearchSurface(initialQuery: String, onLaunch: (AppEntry) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val entries by context.harmoni.appIndex.entries.collectAsState()
    val alphabet by context.harmoni.graffiti.collectAsState()
    var query by remember { mutableStateOf(initialQuery) }
    var lastLetter by remember { mutableStateOf<Char?>(null) }

    // The ring's eight are one tap away already, so by default they do not also take places in a
    // grid of eight. The toggle lives on the launcher app screen for when that is not wanted.
    val overrides by RingSlots.overrides.collectAsState()
    val showRing by RingSlots.showInSearch.collectAsState()
    val hidden by HiddenApps.packages.collectAsState()
    val pool = remember(entries, overrides, showRing, hidden) {
        val visible = entries.visible(hidden)
        if (showRing) {
            visible
        } else {
            val onRing = ringTargets(overrides, visible, hidden).mapNotNull { it?.iconPackage }.toSet()
            visible.filterNot { it.packageName in onRing }
        }
    }

    val results = remember(pool, query) { AppMatcher.match(pool, query) }
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
        animationSpec = tween(durationMillis = EntranceMillis),
        label = "search-entrance",
    )

    // Back is swallowed rather than closing the view. It leaves the ways out that are gestures on
    // the surface itself: tapping away from the grid, and coming home.
    BackHandler {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(entrance)
            // Anywhere that is not the grid, the query or the input area closes the view.
            .noRipple(onClick = onClose),
    ) {
        // Blur under tint, both fading with the entrance, as the design's backdrop filter does.
        BlurredWallpaper(radius = BlurRadius * entrance)

        Box(modifier = Modifier.fillMaxSize().background(Tint))

        Column(modifier = Modifier.fillMaxSize()) {
            QueryLine(query = query)

            ResultSummary(count = results.size)

            HorizontalPager(
                state = pager,
                modifier = Modifier.padding(top = 30.dp),
            ) { page ->
                AppPage(
                    apps = results.drop(page * PerPage).take(PerPage),
                    onLaunch = onLaunch,
                    onInfo = { entry ->
                        val details = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${entry.packageName}".toUri(),
                        )
                        runCatching { context.startActivity(details) }
                    },
                )
            }

            PageDots(pageCount = pageCount, current = pager.currentPage)

            GraffitiInput(
                onLetter = { query += it; lastLetter = it },
                onBackspace = {
                    // Erasing the letter just recognised is the only evidence there is that it was
                    // recognised wrongly. Erasing anything older is ordinary editing.
                    lastLetter?.let { Diagnostics.recordMisread(context, it) }
                    lastLetter = null
                    query = query.dropLast(1)
                },
                recognise = { alphabet.recognise(it)?.letter },
            )
        }
    }
}

/**
 * The query, with a caret where the next letter lands.
 *
 * Plain text rather than a field: there is nothing to focus and no keyboard to raise, and a field
 * would offer both.
 */
@Composable
private fun QueryLine(query: String) {
    Row(
        modifier = Modifier
            .padding(start = 30.dp, end = 30.dp, top = 72.dp)
            .heightIn(min = 46.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = query, style = QueryStyle)

        // Steady rather than blinking: it marks where letters appear, and the search view is
        // already carrying a fade and a stroke trail.
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(2.dp)
                .height(38.dp)
                .background(Color.White.copy(alpha = 0.55f)),
        )
    }
}

@Composable
private fun ResultSummary(count: Int) {
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
    }
}

@Composable
private fun AppPage(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
) {
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
                    AppCell(
                        entry = entry,
                        onLaunch = onLaunch,
                        onInfo = onInfo,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps a short last row aligned with the one above it.
                repeat(Columns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** Tap launches. Holding opens the app's own page in Settings, which is where uninstall lives. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    entry: AppEntry,
    onLaunch: (AppEntry) -> Unit,
    onInfo: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onLaunch(entry) },
            onLongClick = { onInfo(entry) },
        ),
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
                fontSize = 12.sp,
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

/**
 * Where letters are drawn, which Section 4 confines to the space below the grid.
 *
 * The stroke stays on screen and fades once the finger lifts, so a letter that was misread can be
 * seen for what it actually looked like rather than guessed at.
 *
 * A touch too small to be a letter does nothing. This is the writing area, so closing the view out
 * from under a letter that came out short would be its own bug, and it is the one part of the
 * screen where tapping away does not dismiss.
 */
@Composable
private fun ColumnScope.GraffitiInput(
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    recognise: (List<Offset>) -> Char?,
) {
    val density = LocalDensity.current
    val letterSpan = with(density) { LetterSpan.toPx() }
    val backspaceSpan = with(density) { BackspaceSpan.toPx() }

    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var drawing by remember { mutableStateOf(false) }
    val trail = remember { Animatable(0f) }

    LaunchedEffect(drawing) {
        if (drawing) {
            trail.snapTo(1f)
        } else if (live.isNotEmpty()) {
            trail.animateTo(0f, tween(TrailFadeMillis))
            live = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(top = 14.dp)
            .captureStroke(
                key = Unit,
                onProgress = {
                    live = it
                    drawing = true
                },
                onStroke = { points ->
                    drawing = false

                    // A tap in here does nothing. This is the writing area, and closing the view
                    // out from under a letter that came out too small would be its own bug.
                    when {
                        strokeSpan(points) < letterSpan -> Unit
                        isBackspaceStroke(points, backspaceSpan) -> onBackspace()
                        else -> recognise(points)?.let(onLetter)
                    }
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )

        if (live.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(live.first().x, live.first().y)
                    live.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.85f * trail.value),
                    style = Stroke(width = TrailWidth.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** The wallpaper, blurred, as the backdrop the tint sits on. */
@Composable
private fun BlurredWallpaper(radius: Dp) {
    val wallpaper by rememberWallpaper()

    wallpaper?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius, BlurredEdgeTreatment.Unbounded),
            contentScale = ContentScale.Crop,
        )
    }
}

private val Tint = Color(0xFF120E0C).copy(alpha = 0.72f)
private val IconShape = RoundedCornerShape(26)

private val QueryStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 38.sp,
    letterSpacing = 0.04.em,
    color = Color.White,
)

private val MetaStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.12.em,
    color = Color(0xFFCFC6BD),
)

private const val Columns = 4
private const val PerPage = 8

/** Slower than the mockup's 220ms, which was too quick to read against the entrance. */
private const val EntranceMillis = 500

private val BlurRadius = 14.dp

private val LetterSpan = 24.dp
private val BackspaceSpan = 56.dp
private val TrailWidth = 4.dp
private const val TrailFadeMillis = 260
