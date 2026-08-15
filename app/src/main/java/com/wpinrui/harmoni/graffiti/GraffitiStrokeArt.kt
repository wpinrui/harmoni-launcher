package com.wpinrui.harmoni.graffiti

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wpinrui.harmoni.ui.theme.Accent
import kotlin.math.min

/**
 * A captured stroke, drawn to fit whatever box it is given.
 *
 * The dot marks where the stroke started. Direction is half of what makes a unistroke what it is
 * and the one part that cannot be read back from the shape, so a chart without it would be a chart
 * nobody could learn the alphabet from.
 */
@Composable
fun GraffitiStrokeArt(
    points: List<Offset>,
    modifier: Modifier = Modifier,
    colour: Color = Color.White.copy(alpha = 0.92f),
    width: Dp = 2.dp,
) {
    if (points.size < 2) return

    Canvas(modifier = modifier) {
        val fitted = fitTo(points, size)
        val stroke = width.toPx()

        val path = Path().apply {
            moveTo(fitted.first().x, fitted.first().y)
            fitted.drop(1).forEach { lineTo(it.x, it.y) }
        }

        drawPath(path, colour, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawCircle(StartDot, radius = stroke * 1.6f, center = fitted.first())
    }
}

/** Scales into [size] keeping the aspect ratio, which is what tells an l from a wide l. */
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

val StartDot = Accent
