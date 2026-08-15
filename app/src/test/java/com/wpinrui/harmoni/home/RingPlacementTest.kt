package com.wpinrui.harmoni.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The promise is that a tap is never refused, so every case here has to produce a usable centre. */
class RingPlacementTest {

    private val density = Density(2f)
    private val margin = with(density) { RingPlacement.EdgeMargin.toPx() }
    private val phone = Size(1080f, 2400f)

    @Test
    fun `a touch with room to spare is left where it landed`() {
        val landed = Offset(540f, 1200f)
        assertEquals(landed, RingPlacement.clamp(landed, phone, density))
    }

    @Test
    fun `only the axis short of room moves`() {
        val clamped = RingPlacement.clamp(Offset(10f, 1200f), phone, density)

        assertEquals(margin, clamped.x, 0.01f)
        assertEquals(1200f, clamped.y, 0.01f)
    }

    @Test
    fun `a corner moves on both axes`() {
        val clamped = RingPlacement.clamp(Offset(1070f, 2390f), phone, density)

        assertEquals(phone.width - margin, clamped.x, 0.01f)
        assertEquals(phone.height - margin, clamped.y, 0.01f)
    }

    @Test
    fun `an axis with no room at all centres, and the other still clamps`() {
        // Narrower than two margins, so the ring cannot fit horizontally at any position.
        val narrow = Size(margin, 2400f)
        val clamped = RingPlacement.clamp(Offset(0f, 10f), narrow, density)

        assertEquals(narrow.width / 2f, clamped.x, 0.01f)
        assertEquals(margin, clamped.y, 0.01f)
    }

    @Test
    fun `a surface not yet measured produces a real point`() {
        // What `surface` actually holds on the first composition, before onSizeChanged fires.
        val clamped = RingPlacement.clamp(Offset(100f, 200f), Size.Zero, density)

        assertFalse(clamped.x.isNaN())
        assertFalse(clamped.y.isNaN())
        assertTrue(clamped.x.isFinite() && clamped.y.isFinite())
    }
}
