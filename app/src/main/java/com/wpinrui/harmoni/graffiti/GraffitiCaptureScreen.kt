package com.wpinrui.harmoni.graffiti

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wpinrui.harmoni.ui.theme.Karla
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

/**
 * Records the Graffiti alphabet, one letter at a time.
 *
 * Each letter is drawn [SamplesPerLetter] times and every draw is kept as its own template. The
 * slots along the top show what was recorded, direction included, so a bad one can be spotted and
 * redrawn rather than quietly poisoning the alphabet.
 *
 * Everything is written on each change. A capture is a couple of hundred draws and there is no
 * save button to forget.
 */
@Composable
fun GraffitiCaptureScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current

    var samples by remember { mutableStateOf<List<GraffitiSample>>(emptyList()) }
    var letterIndex by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var advanceAfter by remember { mutableStateOf<Char?>(null) }

    // Resumes where the last session stopped rather than at 'a', since this takes several sittings.
    LaunchedEffect(Unit) {
        val stored = withContext(Dispatchers.IO) { GraffitiStore.load(context) }
        samples = stored
        letterIndex = GraffitiLetters
            .indexOfFirst { letter -> stored.count { it.letter == letter } < SamplesPerLetter }
            .coerceAtLeast(0)
    }

    // Keyed on the revision, not on the samples themselves, so the load above cannot round-trip
    // an unreadable file back over the good one it failed to parse.
    LaunchedEffect(revision) {
        if (revision == 0) return@LaunchedEffect
        withContext(Dispatchers.IO) { GraffitiStore.save(context, samples) }
    }

    // A beat after the last sample lands, so the slot filling in is visible before the letter goes.
    LaunchedEffect(advanceAfter) {
        val completed = advanceAfter ?: return@LaunchedEffect
        delay(AdvanceDelayMillis)
        val next = GraffitiLetters.indexOf(completed) + 1
        if (next < GraffitiLetters.size) letterIndex = next
        advanceAfter = null
    }

    val letter = GraffitiLetters[letterIndex]
    val forLetter = remember(samples, letter) { samples.filter { it.letter == letter } }
    val full = forLetter.size >= SamplesPerLetter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding(),
    ) {
        LetterHeader(
            letter = letter,
            letterIndex = letterIndex,
            onPrevious = { if (letterIndex > 0) letterIndex-- },
            onNext = { if (letterIndex < GraffitiLetters.lastIndex) letterIndex++ },
        )

        SampleSlots(
            samples = forLetter,
            onDelete = { index ->
                val target = forLetter[index]
                samples = samples.filterNot { it === target }
                revision++
            },
        )

        DrawArea(
            letter = letter,
            enabled = !full,
            minimumSpan = with(density) { MinimumSpan.toPx() },
            onStroke = { points ->
                samples = samples + GraffitiSample(letter, points)
                revision++
                if (forLetter.size + 1 >= SamplesPerLetter) advanceAfter = letter
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )

        Footer(total = samples.size, path = GraffitiStore.file(context).path)
    }
}

@Composable
private fun LetterHeader(
    letter: Char,
    letterIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Arrow(glyph = "‹", enabled = letterIndex > 0, onClick = onPrevious)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = letter.uppercase(),
                style = TextStyle(
                    fontFamily = Karla,
                    fontWeight = FontWeight.ExtraLight,
                    fontSize = 64.sp,
                    color = Color.White,
                ),
            )
            Text(
                text = "LETTER ${letterIndex + 1} OF ${GraffitiLetters.size}",
                style = MetaStyle,
            )
        }

        Arrow(glyph = "›", enabled = letterIndex < GraffitiLetters.lastIndex, onClick = onNext)
    }
}

@Composable
private fun Arrow(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(50))
            .noRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.Light,
                fontSize = 40.sp,
                color = Color.White.copy(alpha = if (enabled) 0.85f else 0.18f),
            ),
        )
    }
}

/**
 * One slot per sample, filled left to right.
 *
 * The dot marks where the stroke started, which is the part of a unistroke that is impossible to
 * read back from the shape alone and the part most likely to have gone wrong.
 */
@Composable
private fun SampleSlots(samples: List<GraffitiSample>, onDelete: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(SamplesPerLetter) { index ->
            val sample = samples.getOrNull(index)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sample != null) SlotFilled else SlotEmpty)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = if (sample != null) 0.22f else 0.08f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .noRipple(enabled = sample != null) { onDelete(index) },
            ) {
                if (sample != null) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        drawStroke(fitTo(sample.points, size), StrokeWidthThin.toPx())
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawArea(
    letter: Char,
    enabled: Boolean,
    minimumSpan: Float,
    onStroke: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var live by remember(letter) { mutableStateOf<List<Offset>>(emptyList()) }

    // Cleared on the letter as well as on enablement, so the last stroke of one letter does not
    // linger over the next one's empty area.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Canvas0)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .then(
                if (!enabled) Modifier else Modifier.pointerInput(letter) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()

                        val points = mutableListOf(down.position)
                        live = points.toList()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            // Historical points arrive between frames, so the path is as dense as
                            // the digitiser reports rather than as dense as the display refreshes.
                            change.historical.forEach { points += it.position }
                            points += change.position
                            change.consume()
                            live = points.toList()
                            if (!change.pressed) break
                        }

                        if (isStroke(points, minimumSpan)) onStroke(points.toList()) else live = emptyList()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (live.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawStroke(live, StrokeWidthThick.toPx())
            }
        }

        Text(
            text = when {
                !enabled -> "TAP A SAMPLE ABOVE TO REDO IT"
                live.isEmpty() -> "DRAW ${letter.uppercase()} IN ONE STROKE"
                else -> ""
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MetaStyle.copy(textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun Footer(total: Int, path: String) {
    val target = GraffitiLetters.size * SamplesPerLetter

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "$total OF $target SAMPLES", style = MetaStyle)
        Text(
            text = path,
            style = MetaStyle.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.30f)),
        )
    }
}

/**
 * Whether the gesture was a letter rather than a tap or a twitch.
 *
 * Both tests are needed: a slow tap produces plenty of points across a couple of pixels, and a
 * fast flick produces a long path out of four.
 */
private fun isStroke(points: List<Offset>, minimumSpan: Float): Boolean {
    if (points.size < MinimumPoints) return false

    val width = points.maxOf { it.x } - points.minOf { it.x }
    val height = points.maxOf { it.y } - points.minOf { it.y }
    return hypot(width, height) >= minimumSpan
}

/** Scales a stroke into [size], keeping its aspect ratio, which is what tells b from a wide b. */
private fun fitTo(points: List<Offset>, size: Size): List<Offset> {
    val minX = points.minOf { it.x }
    val minY = points.minOf { it.y }
    val width = (points.maxOf { it.x } - minX).coerceAtLeast(1f)
    val height = (points.maxOf { it.y } - minY).coerceAtLeast(1f)

    val scale = min(size.width / width, size.height / height)
    val left = (size.width - width * scale) / 2f
    val top = (size.height - height * scale) / 2f

    return points.map { Offset(left + (it.x - minX) * scale, top + (it.y - minY) * scale) }
}

private fun DrawScope.drawStroke(points: List<Offset>, width: Float) {
    if (points.isEmpty()) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }

    drawPath(path, Color.White.copy(alpha = 0.92f), style = Stroke(width = width))
    drawCircle(StartDot, radius = width * 1.6f, center = points.first())
}

@Composable
private fun Modifier.noRipple(enabled: Boolean, onClick: () -> Unit): Modifier {
    if (!enabled) return this
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

private val Background = Color(0xFF120E0C)
private val Canvas0 = Color(0xFF1D1815)
private val SlotEmpty = Color(0xFF171310)
private val SlotFilled = Color(0xFF221C18)
private val StartDot = Color(0xFFE8B979)

private val StrokeWidthThin = 2.dp
private val StrokeWidthThick = 5.dp
private val MinimumSpan = 24.dp
private const val MinimumPoints = 4
private const val AdvanceDelayMillis = 350L

private val MetaStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.16.em,
    color = Color(0xFF9C918A),
)
