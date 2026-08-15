package com.wpinrui.harmoni.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Where a ring may be summoned, and how big it is when it appears.
 *
 * The margin is not a separate figure: it is the radius plus half an icon plus a little air,
 * which is exactly the distance at which the outermost icon would touch the screen edge. Deriving
 * it means the two cannot drift apart when the radius is tuned.
 *
 * Sections 2 and 3 share this. The contextual ring has the same geometry and the same rule.
 */
object RingPlacement {

    val Radius = 104.dp
    val IconSize = 62.dp
    private val Breathing = 7.dp

    val EdgeMargin = Radius + IconSize / 2 + Breathing

    /**
     * The nearest point to [position] where the whole ring still fits.
     *
     * A touch near an edge moves the centre in rather than being refused: the ring keeps all eight
     * icons on screen and the fixed positions Section 2 depends on, and the finger is never told
     * that where it landed was wrong. Only the axis that runs out of room moves, so a touch near
     * the left edge slides right and not down.
     *
     * On a surface too small to hold the ring on an axis, that axis centres, which is the closest
     * it can get to fitting.
     */
    fun clamp(position: Offset, surface: Size, density: Density): Offset {
        val margin = with(density) { EdgeMargin.toPx() }

        return Offset(
            x = clampAxis(position.x, margin, surface.width),
            y = clampAxis(position.y, margin, surface.height),
        )
    }

    private fun clampAxis(value: Float, margin: Float, extent: Float): Float {
        val high = extent - margin
        return if (margin > high) extent / 2f else value.coerceIn(margin, high)
    }
}
