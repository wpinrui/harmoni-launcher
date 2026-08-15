package com.wpinrui.harmoni.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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

    /** A straight run upward with one finger: whatever shortcut is bound to it. */
    data object SwipeUp : HomeGesture

    /** The same with two fingers down at any point during it. */
    data object TwoFingerSwipeUp : HomeGesture
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
    val swipeTravel = SwipeTravel.toPx()

    // Both halves of a double tap are seen by this one handler, so nothing that appears in
    // between, the ring above all, can take the second one off it.
    var lastTapAt = 0L
    var lastTapPosition = Offset.Unspecified

    awaitEachGesture {
        awaitHomeGesture(touchSlop, longPressTimeout, swipeTravel) { gesture ->
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
    swipeTravel: Float,
    emit: (HomeGesture) -> Unit,
) {
    val down = awaitFirstDown(requireUnconsumed = true)
    val path = mutableListOf(down.position)
    var moved = false
    var released = false

    // Counted across the whole gesture rather than sampled at the end, since the second finger
    // usually lifts first and the count would be back to one by the time anything is decided.
    var fingers = 1

    // The long-press window decides between the two still gestures. Moving inside it means the
    // touch was never still, so it is a stroke regardless of how long the finger stays down.
    val settled = withTimeoutOrNull(longPressTimeout) {
        while (true) {
            val change = nextChange(down.id) { fingers = maxOf(fingers, it) } ?: break
            path += change.trail()
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
                val change = nextChange(down.id) { fingers = maxOf(fingers, it) } ?: break
                path += change.trail()
                if (!change.pressed) released = true
            }

            val points = path.toList()
            val swipedUp = isSwipeUp(points, swipeTravel)

            when {
                fingers >= 2 && swipedUp -> emit(HomeGesture.TwoFingerSwipeUp)
                // Two fingers doing anything else is not a letter. Nobody writes with two.
                fingers >= 2 -> Unit
                swipedUp -> emit(HomeGesture.SwipeUp)
                else -> emit(HomeGesture.Stroke(points))
            }
        }
    }
}

private fun isSwipeUp(points: List<Offset>, minimumTravel: Float) =
    isStraightSwipe(points, minimumTravel, downward = false)

/**
 * Whether the path was a deliberate run along one axis rather than a letter.
 *
 * Straightness is what separates an upward swipe from a letter, and by a wide margin: measured
 * over the captured alphabet, every letter that ends above where it started is an arch or a curve
 * and scores 0.34 or below.
 *
 * Downward is a different matter, since i is a straight vertical at 0.99, so nothing here can tell
 * the two apart. Where the stroke starts is what settles that, and the surface decides it.
 */
internal fun isStraightSwipe(
    points: List<Offset>,
    minimumTravel: Float,
    downward: Boolean,
): Boolean {
    if (points.size < 2) return false

    val delta = points.last() - points.first()
    if (if (downward) delta.y <= 0f else delta.y >= 0f) return false

    val direct = delta.getDistance()
    if (direct < minimumTravel) return false
    if (abs(delta.y) < VerticalBias * abs(delta.x)) return false

    val travelled = points.zipWithNext().fold(0f) { total, (a, b) -> total + (b - a).getDistance() }
    return travelled > 0f && direct / travelled >= Straightness
}

/** Far enough that it cannot be a flick, short enough to stay inside the surface. */
internal val SwipeTravel = 72.dp

private const val VerticalBias = 2f
private const val Straightness = 0.85f

/**
 * The first pointer's change from the next event, reporting how many fingers that event carried.
 *
 * Null once that pointer is gone from the stream, which is what ends every loop here.
 */
private suspend inline fun AwaitPointerEventScope.nextChange(
    id: PointerId,
    onFingers: (Int) -> Unit,
): PointerInputChange? {
    val event = awaitPointerEvent()
    onFingers(event.changes.count { it.pressed })
    return event.changes.firstOrNull { it.id == id }
}

/**
 * Every position this change carries, not just the latest.
 *
 * The digitiser samples faster than the display refreshes, and the points in between are what a
 * curve is made of. One point per frame is enough to tell a stroke from a tap, but the recogniser
 * is comparing shapes against templates captured with the whole trail.
 */
private fun PointerInputChange.trail(): List<Offset> =
    historical.map { it.position } + position

private suspend fun AwaitPointerEventScope.awaitRelease(id: PointerId) {
    while (true) {
        val change = nextChange(id) {} ?: return
        if (!change.pressed) return
    }
}
