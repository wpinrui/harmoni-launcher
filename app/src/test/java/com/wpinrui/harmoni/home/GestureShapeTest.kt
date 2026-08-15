package com.wpinrui.harmoni.home

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.wpinrui.harmoni.graffiti.isBackspaceStroke

/**
 * The shape tests that stand between a gesture and a letter.
 *
 * Android's y grows downward, so the sign convention here is the whole of the difference between
 * a swipe up and a swipe down, and inverting it is a one-character change that compiles.
 */
class GestureShapeTest {

    private val travel = 72f
    private val span = 56f

    @Test
    fun `a straight run upward is a swipe up and not a swipe down`() {
        val points = line(Offset(100f, 400f), Offset(100f, 100f))

        assertTrue(isStraightSwipe(points, travel, downward = false))
        assertFalse(isStraightSwipe(points, travel, downward = true))
    }

    @Test
    fun `the same points cannot be both directions`() {
        val down = line(Offset(100f, 100f), Offset(100f, 400f))

        assertTrue(isStraightSwipe(down, travel, downward = true))
        assertFalse(isStraightSwipe(down, travel, downward = false))
    }

    @Test
    fun `a stroke that ends level is neither`() {
        val flat = line(Offset(100f, 200f), Offset(400f, 200f))

        assertFalse(isStraightSwipe(flat, travel, downward = true))
        assertFalse(isStraightSwipe(flat, travel, downward = false))
    }

    @Test
    fun `a short run is not a swipe`() {
        assertFalse(isStraightSwipe(line(Offset(0f, 60f), Offset(0f, 0f)), travel, downward = false))
    }

    @Test
    fun `a diagonal is not vertical enough`() {
        // Equal dx and dy, so it fails the two-to-one bias before straightness is considered.
        assertFalse(isStraightSwipe(line(Offset(0f, 300f), Offset(300f, 0f)), travel, downward = false))
    }

    @Test
    fun `an arch that ends high is not a swipe up`() {
        // The shape of the letter n, which ends above where it started and must stay a letter.
        val arch = line(Offset(0f, 300f), Offset(0f, 0f)) +
            line(Offset(0f, 0f), Offset(120f, 0f)) +
            line(Offset(120f, 0f), Offset(120f, 260f))

        assertFalse(isStraightSwipe(arch, travel, downward = false))
    }

    @Test
    fun `fewer than two points is never a swipe`() {
        assertFalse(isStraightSwipe(emptyList(), travel, downward = false))
        assertFalse(isStraightSwipe(listOf(Offset(1f, 1f)), travel, downward = false))
    }

    @Test
    fun `a flat run right to left is backspace`() {
        assertTrue(isBackspaceStroke(line(Offset(400f, 200f), Offset(100f, 200f)), span))
    }

    @Test
    fun `left to right is not backspace`() {
        assertFalse(isBackspaceStroke(line(Offset(100f, 200f), Offset(400f, 200f)), span))
    }

    @Test
    fun `a short leftward tick is not backspace`() {
        assertFalse(isBackspaceStroke(line(Offset(140f, 200f), Offset(100f, 200f)), span))
    }

    @Test
    fun `a stroke that doubles back is not backspace`() {
        val there = line(Offset(100f, 200f), Offset(400f, 200f))
        val andBack = line(Offset(400f, 200f), Offset(120f, 200f))

        // Net travel is a fraction of the span it covered, so it is a letter, not an erase.
        assertFalse(isBackspaceStroke(there + andBack, span))
    }

    @Test
    fun `a tall leftward stroke is not backspace`() {
        assertFalse(isBackspaceStroke(line(Offset(400f, 100f), Offset(100f, 350f)), span))
    }

    @Test
    fun `fewer than two points is never backspace`() {
        assertFalse(isBackspaceStroke(listOf(Offset(1f, 1f)), span))
    }

    private fun line(from: Offset, to: Offset, steps: Int = 30) = List(steps) { step ->
        val t = step / (steps - 1f)
        Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
    }
}
