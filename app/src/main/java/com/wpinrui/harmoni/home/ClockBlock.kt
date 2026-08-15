package com.wpinrui.harmoni.home

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wpinrui.harmoni.apps.AppIcon
import com.wpinrui.harmoni.system.NotificationCounts
import com.wpinrui.harmoni.system.NowPlaying
import com.wpinrui.harmoni.system.rememberBatteryPercent
import com.wpinrui.harmoni.system.rememberClock
import com.wpinrui.harmoni.system.rememberIs24Hour
import com.wpinrui.harmoni.system.rememberNowPlaying
import com.wpinrui.harmoni.ui.theme.Karla
import com.wpinrui.harmoni.ui.theme.noRipple
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The one composed block on the home surface: clock, date, battery, notification badges, the
 * music element and the YouTube link, as a single visual unit.
 *
 * Measurements came from the design mockups, which the built launcher has since replaced as the
 * source of truth. Everything outside the block is left free
 * for the wallpaper, ring taps and Graffiti strokes, so the block claims no more width than its
 * content and takes touches only on the rows that are actually bound to something.
 *
 * The block is drawn twice. The first pass is the same content blacked out, blurred and dropped a
 * few dp, which is the shadow; the second is the block itself. A text shadow only reaches text,
 * and the icons need to lift off the wallpaper just as much as the clock does.
 */
@Composable
fun ClockBlock(modifier: Modifier = Modifier) {
    val state = BlockState(
        time = rememberClock().value,
        is24Hour = rememberIs24Hour(),
        battery = rememberBatteryPercent().value,
        counts = NotificationCounts.counts.collectAsState().value,
        nowPlaying = rememberNowPlaying().value,
    )

    Box(modifier = modifier) {
        BlockContent(
            state = state,
            shadowPass = true,
            modifier = Modifier
                .offset(y = ShadowDrop)
                .blur(ShadowBlur, BlurredEdgeTreatment.Unbounded)
                .silhouette(ShadowAlpha),
        )
        BlockContent(state = state, shadowPass = false)
    }
}

/** Everything the block draws, read once and handed to both passes. */
private data class BlockState(
    val time: LocalDateTime,
    val is24Hour: Boolean,
    val battery: Int,
    val counts: Map<String, Int>,
    val nowPlaying: NowPlaying?,
)

@Composable
private fun BlockContent(
    state: BlockState,
    shadowPass: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 30.dp, top = 74.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        TimeAndStatus(state, shadowPass)
        Badges(state.counts, shadowPass)
        MusicAndLink(state.nowPlaying, shadowPass)
    }
}

@Composable
private fun TimeAndStatus(state: BlockState, shadowPass: Boolean) {
    // Read from the composition rather than from Locale.getDefault(), which is not observable:
    // changing the system language would otherwise leave the date in the old one until something
    // else forced a recomposition.
    val locale = LocalLocale.current.platformLocale
    val timeFormat = remember(state.is24Hour, locale) {
        DateTimeFormatter.ofPattern(if (state.is24Hour) "H:mm" else "h:mm", locale)
    }
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM", locale) }

    val context = LocalContext.current

    // Each element opens what it is about. The shadow pass takes no touches, so a tap always
    // lands on the element you can read rather than the one drawn underneath it.
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = state.time.format(timeFormat),
            modifier = Modifier
                // The mockup pulls the clock 5px left off the block's edge. Compose rejects
                // negative padding, so the nudge is an offset.
                .offset(x = (-5).dp)
                // At 80sp the same shadow that makes 11sp legible reads as a smear, so the
                // clock's share of the silhouette is dialled back.
                .alpha(if (shadowPass) ClockShadowShare else 1f)
                .noRipple(enabled = !shadowPass) { context.launchOrLog(clockIntent(), "the clock") },
            style = ClockStyle,
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = state.time.format(dateFormat).uppercase(locale),
                modifier = Modifier.noRipple(enabled = !shadowPass) {
                    context.launchOrLog(calendarIntent(), "the calendar")
                },
                style = CapsStyle,
            )
            Row(
                modifier = Modifier
                    .padding(top = 11.dp)
                    .noRipple(enabled = !shadowPass) { context.launchOrLog(batteryIntent(), "battery settings") },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BatteryGlyph()
                Text(text = "${state.battery}%", style = CapsStyle)
            }
        }
    }
}

/** The alarms screen, which is the standard way to ask for whatever clock app is installed. */
private fun clockIntent() = Intent(AlarmClock.ACTION_SHOW_ALARMS)

/** Addressed by instant rather than by package, so it opens on today in whichever calendar. */
private fun calendarIntent(): Intent {
    val today = CalendarContract.CONTENT_URI.buildUpon()
        .appendPath("time")
        .appendPath(System.currentTimeMillis().toString())
        .build()
    return Intent(Intent.ACTION_VIEW, today)
}

private fun batteryIntent() = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)

@Composable
private fun Badges(counts: Map<String, Int>, shadowPass: Boolean) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeBindings.badged.forEachIndexed { index, badge ->
            val count = counts[badge.packageName] ?: 0

            Row(
                modifier = Modifier
                    .noRipple(enabled = !shadowPass) { context.launchApp(badge.packageName) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(badge.icon),
                    contentDescription = null,
                    modifier = Modifier.size(BadgeIconSize),
                )
                // Nothing at zero, not even reserved width. The count keeps a floor so a badge
                // does not jump as it crosses from one digit to two, and stops at 99+ so a noisy
                // app cannot push the row wider than the block.
                if (count > 0) {
                    Text(
                        text = if (count > MaxBadgeCount) "$MaxBadgeCount+" else count.toString(),
                        modifier = Modifier.widthIn(min = 18.dp),
                        style = CountStyle,
                    )
                }
            }

            // A numbered badge needs air before the next icon. A bare one only needs enough to
            // read as a separate icon, so it takes half an icon width.
            if (index != HomeBindings.badged.lastIndex) {
                Spacer(Modifier.width(if (count > 0) 20.dp else BadgeIconSize / 2))
            }
        }
    }
}

@Composable
private fun MusicAndLink(nowPlaying: NowPlaying?, shadowPass: Boolean) {
    val context = LocalContext.current

    Column(modifier = Modifier.offset(y = (-9).dp)) {
        Row(
            modifier = Modifier
                .noRipple(enabled = !shadowPass) { context.launchApp(HomeBindings.YOUTUBE_MUSIC) }
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusicGlyph(playing = nowPlaying != null)
            // Idle is a label, so it matches YOUTUBE. Playing is a track name, which cannot be
            // uppercased without mangling it, so it keeps its own style.
            Text(
                text = nowPlaying?.line ?: "MUSIC",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 300.dp),
                style = if (nowPlaying == null) LinkLabelStyle else TrackStyle,
            )
        }

        Row(
            modifier = Modifier
                .noRipple(enabled = !shadowPass) { context.launchApp(HomeBindings.YOUTUBE) }
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = HomeBindings.YOUTUBE, size = BadgeIconSize)
            Text(text = "YOUTUBE", style = LinkLabelStyle)
        }
    }
}

/** Bars while something plays, a quaver while nothing does. */
@Composable
private fun MusicGlyph(playing: Boolean) {
    Canvas(modifier = Modifier.size(BadgeIconSize)) {
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = size.minDimension / 2 - 0.5.dp.toPx(),
            style = Stroke(width = 1.dp.toPx()),
        )

        val centre = Offset(size.width / 2, size.height / 2)
        if (playing) {
            val barWidth = 2.dp.toPx()
            val gap = 2.5.dp.toPx()
            val heights = listOf(9.dp.toPx(), 13.dp.toPx(), 6.dp.toPx())
            val totalWidth = heights.size * barWidth + (heights.size - 1) * gap
            var x = centre.x - totalWidth / 2
            heights.forEach { height ->
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(x, centre.y - height / 2),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
                x += barWidth + gap
            }
        } else {
            // A quaver: tilted notehead, stem rising out of its right shoulder, flag hooking off
            // the top. The stem runs down into the head so the two are one shape rather than a
            // line sitting near an oval.
            val headWidth = 7.dp.toPx()
            val headHeight = 5.dp.toPx()
            val stemWidth = 1.4.dp.toPx()
            val headCentre = Offset(centre.x - 2.2.dp.toPx(), centre.y + 3.5.dp.toPx())
            val stemX = headCentre.x + headWidth / 2 - stemWidth / 2
            val stemTop = centre.y - 6.5.dp.toPx()

            drawLine(
                color = Color.White,
                start = Offset(stemX, headCentre.y),
                end = Offset(stemX, stemTop),
                strokeWidth = stemWidth,
                cap = StrokeCap.Round,
            )

            val flag = Path().apply {
                moveTo(stemX, stemTop)
                quadraticTo(
                    stemX + 5.5.dp.toPx(), stemTop + 1.5.dp.toPx(),
                    stemX + 3.dp.toPx(), stemTop + 6.dp.toPx(),
                )
            }
            drawPath(
                path = flag,
                color = Color.White,
                style = Stroke(width = stemWidth, cap = StrokeCap.Round),
            )

            rotate(degrees = -20f, pivot = headCentre) {
                drawOval(
                    color = Color.White,
                    topLeft = Offset(headCentre.x - headWidth / 2, headCentre.y - headHeight / 2),
                    size = Size(headWidth, headHeight),
                )
            }
        }
    }
}

@Composable
private fun BatteryGlyph() {
    Canvas(modifier = Modifier.size(width = 20.dp, height = 10.dp)) {
        val bodyWidth = 17.dp.toPx()
        drawRoundRect(
            color = Color.White,
            size = Size(bodyWidth, size.height),
            cornerRadius = CornerRadius(2.5.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height / 2 - 2.5.dp.toPx()),
            size = Size(2.5.dp.toPx(), 5.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}

/** Replaces every colour with black, keeping the alpha, so the pass is a shape and not a copy. */
private fun Modifier.silhouette(alpha: Float) = graphicsLayer {
    this.alpha = alpha
    renderEffect = android.graphics.RenderEffect
        .createColorFilterEffect(
            PorterDuffColorFilter(android.graphics.Color.BLACK, PorterDuff.Mode.SRC_IN),
        )
        .asComposeRenderEffect()
}

/** The clock's contribution to the silhouette, relative to everything else. */
private const val ClockShadowShare = 0.4f

private val ShadowDrop = 3.dp
private val ShadowBlur = 12.dp
private const val ShadowAlpha = 0.8f

/** Lighter than the rest: 80sp glyphs carry their own weight against a photo. */
private val ClockStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 80.sp,
    lineHeight = 0.86.em,
    letterSpacing = (-0.035).em,
    color = Color.White,
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.3f),
        offset = Offset(0f, 2f),
        blurRadius = 16f,
    ),
)

/**
 * The text shadow, on top of the silhouette pass.
 *
 * The silhouette is a wide halo; this is the tighter edge that keeps small text crisp against a
 * busy photo.
 */
private val BlockShadow = Shadow(
    color = Color.Black.copy(alpha = 0.55f),
    offset = Offset(0f, 3f),
    blurRadius = 26f,
)

/** Date and battery. Full white rather than 92%, because they were the hardest line to read. */
private val CapsStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.24.em,
    color = Color.White,
    shadow = BlockShadow,
)

/** The caps label shared by the music element when idle and the YouTube link. */
private val LinkLabelStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.26.em,
    color = Color.White,
    shadow = BlockShadow,
)

/** Track names keep their own casing and a larger size, since they are content, not a label. */
private val TrackStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Light,
    fontSize = 14.sp,
    letterSpacing = 0.03.em,
    color = Color.White,
    shadow = BlockShadow,
)

private val CountStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.04.em,
    color = Color.White,
    shadow = BlockShadow,
)

private val BadgeIconSize = 24.dp
private const val MaxBadgeCount = 99
