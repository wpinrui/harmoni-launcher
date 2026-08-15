package com.wpinrui.harmoni.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What a touch on the empty surface turned out to be.
 *
 * The three are told apart by path and time, nothing else. A tap has no path and a letter does, so
 * the ring and Graffiti never compete for the same gesture.
 */
sealed interface HomeGesture {

    /** Down and up in one place: summon the ring. */
    data class Tap(val position: Offset) : HomeGesture

    /** Held in one place past the long-press timeout: summon the contextual ring. */
    data class LongPress(val position: Offset) : HomeGesture

    /** Moved beyond touch slop: a Graffiti stroke, from first point to last. */
    data class Stroke(val points: List<Offset>) : HomeGesture
}

/**
 * Classifies every touch on the surface and hands the result to [onGesture].
 *
 * Uses `requireUnconsumed`, so anything a child took first, such as a badge or the YouTube link,
 * never reaches here. That also settles the open question in Section 1 for now: a stroke that
 * starts on the clock block does not register.
 */
fun Modifier.homeGestures(onGesture: (HomeGesture) -> Unit): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        val gesture = awaitHomeGesture(touchSlop, longPressTimeout)
        if (gesture != null) onGesture(gesture)
    }
}

private suspend fun AwaitPointerEventScope.awaitHomeGesture(
    touchSlop: Float,
    longPressTimeout: Long,
): HomeGesture? {
    val down = awaitFirstDown(requireUnconsumed = true)
    val path = mutableListOf(down.position)
    var moved = false
    var released = false

    // The long-press window decides between the two still gestures. Moving inside it means the
    // touch was never still, so it is a stroke regardless of how long the finger stays down.
    val settled = withTimeoutOrNull(longPressTimeout) {
        while (true) {
            val change = currentEvent(down.id) ?: break
            path += change.position
            if (!change.pressed) {
                released = true
                break
            }
            if ((change.position - down.position).getDistance() > touchSlop) {
                moved = true
                break
            }
        }
    }

    return when {
        // Timed out with the finger still down and still: the contextual ring.
        settled == null && !moved && !released -> {
            awaitRelease(down.id)
            HomeGesture.LongPress(down.position)
        }

        released && !moved -> HomeGesture.Tap(down.position)

        else -> {
            while (!released) {
                val change = currentEvent(down.id) ?: break
                path += change.position
                if (!change.pressed) released = true
            }
            HomeGesture.Stroke(path.toList())
        }
    }
}

private suspend fun AwaitPointerEventScope.currentEvent(id: PointerId) =
    awaitPointerEvent().changes.firstOrNull { it.id == id }

private suspend fun AwaitPointerEventScope.awaitRelease(id: PointerId) {
    while (true) {
        val change = currentEvent(id) ?: return
        if (!change.pressed) return
    }
}
