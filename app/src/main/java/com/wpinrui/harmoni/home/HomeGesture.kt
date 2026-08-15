package com.wpinrui.harmoni.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
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

    /** A second tap in the same place, soon after the first: the all apps view. */
    data class DoubleTap(val position: Offset) : HomeGesture

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
    // Two thirds of the system's, which is tuned for pressing something rather than for a
    // gesture whose whole job is to be quick.
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis * 2 / 3
    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis

    // Both halves of a double tap are seen by this one handler, so nothing that appears in
    // between, the ring above all, can take the second one off it.
    var lastTapAt = 0L
    var lastTapPosition = Offset.Unspecified

    awaitEachGesture {
        awaitHomeGesture(touchSlop, longPressTimeout) { gesture ->
            if (gesture !is HomeGesture.Tap) {
                lastTapAt = 0L
                onGesture(gesture)
                return@awaitHomeGesture
            }

            val now = android.os.SystemClock.uptimeMillis()
            val soon = now - lastTapAt <= doubleTapTimeout
            val nearby = lastTapPosition.isSpecified &&
                (gesture.position - lastTapPosition).getDistance() <= doubleTapSlop

            if (soon && nearby) {
                // Spent, so a third tap starts a fresh pair rather than continuing this one.
                lastTapAt = 0L
                onGesture(HomeGesture.DoubleTap(gesture.position))
            } else {
                lastTapAt = now
                lastTapPosition = gesture.position
                onGesture(gesture)
            }
        }
    }
}

/**
 * How far the second tap may drift from the first.
 *
 * Wider than touch slop, because the second tap is aimed from memory at a place the finger has
 * already left, rather than tracked against something on screen.
 */
private val doubleTapSlop = 96f

/**
 * Emits through [emit] rather than returning, because the three gestures resolve at different
 * moments. A long press is known the instant the threshold passes and is reported there, so the
 * ring is up while the finger is still down. A tap and a stroke are only known on release.
 */
private suspend fun AwaitPointerEventScope.awaitHomeGesture(
    touchSlop: Float,
    longPressTimeout: Long,
    emit: (HomeGesture) -> Unit,
) {
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

    when {
        // Timed out with the finger still down and still. Report it now, then swallow the rest
        // of the press so lifting off does not read as a second gesture.
        settled == null && !moved && !released -> {
            emit(HomeGesture.LongPress(down.position))
            awaitRelease(down.id)
        }

        released && !moved -> emit(HomeGesture.Tap(down.position))

        else -> {
            while (!released) {
                val change = currentEvent(down.id) ?: break
                path += change.position
                if (!change.pressed) released = true
            }
            emit(HomeGesture.Stroke(path.toList()))
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
