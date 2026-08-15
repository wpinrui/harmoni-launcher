package com.wpinrui.harmoni.graffiti

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

/**
 * Collects one finger's path, and hands it over when the finger lifts.
 *
 * [onProgress] is called as the stroke is drawn, for whoever wants to show the ink; [onStroke] is
 * called once at the end. Both surfaces that take handwriting use this, so a change to how a
 * stroke is gathered reaches both.
 *
 * [key] restarts the gesture handler, and must be whatever identifies the stroke being captured.
 * Anything the callbacks read from composition has to be either passed through or read from live
 * state: the handler is not relaunched on recomposition, so a plain captured value goes stale.
 */
fun Modifier.captureStroke(
    key: Any?,
    onProgress: (List<Offset>) -> Unit = {},
    onStroke: (List<Offset>) -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()

        val points = mutableListOf(down.position)
        onProgress(points.toList())

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            // Historical points arrive between frames, so the path is as dense as the digitiser
            // reports rather than as dense as the display refreshes.
            change.historical.forEach { points += it.position }
            points += change.position
            change.consume()
            onProgress(points.toList())
            if (!change.pressed) break
        }

        onStroke(points.toList())
    }
}

/** The diagonal of a stroke's bounding box, which is how far the finger actually got. */
fun strokeSpan(points: List<Offset>): Float {
    if (points.size < 2) return 0f
    val width = points.maxOf { it.x } - points.minOf { it.x }
    val height = points.maxOf { it.y } - points.minOf { it.y }
    return hypot(width, height)
}
