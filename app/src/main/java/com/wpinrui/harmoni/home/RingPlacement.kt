package com.wpinrui.harmoni.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Where a ring may be summoned, and how big it is when it appears.
 *
 * The numbers come from `design/LauncherPhone.dc.html`. The margin is not a separate figure there:
 * it is the radius plus half an icon plus a little air, which is exactly the distance at which the
 * outermost icon would touch the screen edge. Deriving it means the two cannot drift apart when
 * the radius is tuned.
 *
 * Sections 2 and 3 share this. The contextual ring has the same geometry and the same rule.
 */
object RingPlacement {

    val Radius = 104.dp
    val IconSize = 62.dp
    private val Breathing = 7.dp

    val EdgeMargin = Radius + IconSize / 2 + Breathing

    /**
     * Whether the full ring fits around [position].
     *
     * A tap too near an edge does not register at all, rather than summoning a ring with icons
     * pushed off screen or shuffled inward, which would break the fixed positions Section 2
     * depends on.
     */
    fun fits(position: Offset, surface: Size, density: Density): Boolean {
        val margin = with(density) { EdgeMargin.toPx() }
        return position.x >= margin &&
            position.y >= margin &&
            position.x <= surface.width - margin &&
            position.y <= surface.height - margin
    }
}
