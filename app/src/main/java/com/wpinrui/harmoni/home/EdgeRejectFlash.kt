package com.wpinrui.harmoni.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * The flash shown when a tap lands too near an edge for a ring to fit.
 *
 * Without it a rejected tap is indistinguishable from a tap the launcher missed, and Section 5
 * wants to count rejections precisely because they are a usability problem worth seeing.
 *
 * A rim of warm light around the edge, brightening fast and fading slowly, taken from the mockup.
 */
@Composable
fun EdgeRejectFlash(rejectCount: Int, modifier: Modifier = Modifier) {
    val glow = remember { Animatable(0f) }

    LaunchedEffect(rejectCount) {
        if (rejectCount == 0) return@LaunchedEffect
        glow.snapTo(0f)
        glow.animateTo(1f, tween(durationMillis = 125))
        glow.animateTo(0f, tween(durationMillis = 375))
    }

    if (glow.value <= 0f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = glow.value

        // Inward fade from the edges, which is what an inset glow amounts to.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, RejectGlow.copy(alpha = 0.35f * alpha)),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension * 0.62f,
            ),
        )

        drawRect(
            color = RejectGlow.copy(alpha = 0.9f * alpha),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private val RejectGlow = Color(0xFFFF785A)
