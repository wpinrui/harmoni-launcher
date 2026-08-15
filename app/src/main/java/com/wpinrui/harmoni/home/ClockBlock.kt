package com.wpinrui.harmoni.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.wpinrui.harmoni.apps.AppIcon
import com.wpinrui.harmoni.system.NotificationCounts
import com.wpinrui.harmoni.system.rememberBatteryPercent
import com.wpinrui.harmoni.system.rememberClock
import com.wpinrui.harmoni.system.rememberIs24Hour
import com.wpinrui.harmoni.system.rememberNowPlaying
import com.wpinrui.harmoni.ui.theme.Karla
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The one composed block on the home surface: clock, date, battery, notification badges, the
 * music element and the YouTube link, as a single visual unit.
 *
 * Measurements follow `design/LauncherPhone.dc.html`. Everything outside the block is left free
 * for the wallpaper, ring taps and Graffiti strokes, so the block claims no more width than its
 * content and takes touches only on the rows that are actually bound to something.
 */
@Composable
fun ClockBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(start = 30.dp, top = 74.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        TimeAndStatus()
        Badges()
        MusicAndLink()
    }
}

@Composable
private fun TimeAndStatus() {
    val time by rememberClock()
    val battery by rememberBatteryPercent()
    val is24Hour = rememberIs24Hour()

    val timeFormat = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "H:mm" else "h:mm", Locale.getDefault())
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = time.format(timeFormat),
            modifier = Modifier.padding(start = (-5).dp),
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.ExtraLight,
                fontSize = 80.sp,
                lineHeight = 0.86.em,
                letterSpacing = (-0.035).em,
                color = Color.White,
                shadow = BlockShadow,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = time.format(dateFormat).uppercase(Locale.getDefault()),
                style = CapsStyle,
            )
            Row(
                modifier = Modifier.padding(top = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BatteryGlyph()
                Text(text = "$battery%", style = CapsStyle)
            }
        }
    }
}

@Composable
private fun Badges() {
    val context = LocalContext.current
    val counts by NotificationCounts.counts.collectAsState()

    Row(
        modifier = Modifier.padding(top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeBindings.badged.forEach { packageName ->
            Row(
                modifier = Modifier
                    .tappable { context.launchApp(packageName) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(packageName = packageName, size = 24.dp)
                // The number is dropped at zero rather than shown as "0", but its width is held
                // so the badges do not shuffle sideways as notifications come and go.
                Text(
                    text = counts[packageName]?.takeIf { it > 0 }?.toString().orEmpty(),
                    modifier = Modifier.widthIn(min = 18.dp),
                    style = CountStyle,
                )
            }
        }
    }
}

@Composable
private fun MusicAndLink() {
    val context = LocalContext.current
    val nowPlaying by rememberNowPlaying()

    Column(modifier = Modifier.padding(vertical = (-9).dp)) {
        Row(
            modifier = Modifier
                .tappable { context.launchApp(HomeBindings.YOUTUBE_MUSIC) }
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusicGlyph(playing = nowPlaying != null)
            Text(
                text = nowPlaying?.line ?: "Music",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 300.dp),
                style = TextStyle(
                    fontFamily = Karla,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    letterSpacing = 0.03.em,
                    color = Color.White,
                    shadow = BlockShadow,
                ),
            )
        }

        Row(
            modifier = Modifier
                .tappable { context.launchApp(HomeBindings.YOUTUBE) }
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = HomeBindings.YOUTUBE, size = 24.dp)
            Text(
                text = "YOUTUBE",
                style = TextStyle(
                    fontFamily = Karla,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 0.26.em,
                    color = Color.White.copy(alpha = 0.92f),
                    shadow = BlockShadow,
                ),
            )
        }
    }
}

/** Bars while something plays, a note while nothing does. */
@Composable
private fun MusicGlyph(playing: Boolean) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = size.minDimension / 2 - 0.5.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
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
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2),
                    )
                    x += barWidth + gap
                }
            } else {
                val stemX = centre.x + 2.5.dp.toPx()
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(stemX, centre.y - 6.dp.toPx()),
                    size = Size(1.5.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(stemX, centre.y - 6.dp.toPx()),
                    size = Size(4.dp.toPx(), 1.5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(centre.x - 5.5.dp.toPx(), centre.y + 1.5.dp.toPx()),
                    size = Size(6.dp.toPx(), 4.5.dp.toPx()),
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
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyWidth + 1.dp.toPx(), size.height / 2 - 2.5.dp.toPx()),
            size = Size(2.5.dp.toPx(), 5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
        )
    }
}

/** Tappable without a ripple: the block sits on a wallpaper, and a ripple would smear over it. */
@Composable
private fun Modifier.tappable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

private val BlockShadow = Shadow(
    color = Color.Black.copy(alpha = 0.5f),
    offset = Offset(0f, 2f),
    blurRadius = 14f,
)

private val CapsStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.24.em,
    color = Color.White.copy(alpha = 0.92f),
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
