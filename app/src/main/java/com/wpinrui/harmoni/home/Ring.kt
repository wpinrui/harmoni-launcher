package com.wpinrui.harmoni.home

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wpinrui.harmoni.apps.AppIcon
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The eight app ring, centred on where the finger landed.
 *
 * The same ring serves Sections 2 and 3: the fixed eight on a tap, a scored eight on a long press.
 *
 * Two-stage by construction: the tap that summoned it is already spent, so the next tap is the
 * pick. The centre dismisses, and so does anywhere outside an icon, since a ring you cannot get
 * rid of by tapping away would be a trap.
 *
 * Icons grow out of the centre with a short stagger, as in `design/LauncherPhone.dc.html`. The
 * mockup blurs what is behind each icon; Compose cannot blur a backdrop without capturing it
 * first, so these are translucent instead.
 */
@Composable
fun Ring(
    centre: Offset,
    slots: List<RingTarget>,
    onPick: (RingTarget) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(centre) { appeared = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .noRipple(onDismiss),
    ) {
        CentreButton(centre = centre, appeared = appeared, onDismiss = onDismiss)

        slots.forEachIndexed { index, target ->
            val progress by animateFloatAsState(
                targetValue = if (appeared) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 260,
                    delayMillis = index * 14,
                    easing = RingEasing,
                ),
                label = "ring-slot-$index",
            )

            RingIcon(
                target = target,
                centre = centre,
                index = index,
                progress = progress,
                onPick = onPick,
            )
        }
    }
}

@Composable
private fun RingIcon(
    target: RingTarget,
    centre: Offset,
    index: Int,
    progress: Float,
    onPick: (RingTarget) -> Unit,
) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(0) }

    // Icons start bunched near the centre and travel out, rather than fading in where they land.
    val reach = with(density) { RingPlacement.Radius.toPx() } * (CollapsedReach + (1f - CollapsedReach) * progress)
    val angle = Math.toRadians(-90.0 + index * 45.0)
    val position = Offset(
        x = centre.x + (cos(angle) * reach).toFloat(),
        y = centre.y + (sin(angle) * reach).toFloat(),
    )

    Box(
        modifier = Modifier
            .offset { IntOffset((position.x - size / 2).roundToInt(), (position.y - size / 2).roundToInt()) }
            .size(RingPlacement.IconSize)
            .onSizeChanged { size = it.width }
            .scale(CollapsedScale + (1f - CollapsedScale) * progress)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .noRipple { onPick(target) },
        contentAlignment = Alignment.Center,
    ) {
        val bundled = (target as? RingTarget.Web)?.icon
        if (bundled != null) {
            Image(
                painter = painterResource(bundled),
                contentDescription = target.label,
                modifier = Modifier.size(IconArtSize),
            )
        } else {
            AppIcon(packageName = target.iconPackage, size = IconArtSize)
        }
    }
}

@Composable
private fun CentreButton(centre: Offset, appeared: Boolean, onDismiss: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = tween(durationMillis = 220, easing = RingEasing),
        label = "ring-centre",
    )
    var size by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .offset { IntOffset((centre.x - size / 2).roundToInt(), (centre.y - size / 2).roundToInt()) }
            .size(CentreSize)
            .onSizeChanged { size = it.width }
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .noRipple(onDismiss),
    )
}

/** No ripple anywhere in the ring: it sits on a wallpaper and a ripple would smear across it. */
@Composable
private fun Modifier.noRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

private val RingEasing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
private val CentreSize = 52.dp
private val IconArtSize = 40.dp
private const val CollapsedReach = 0.12f
private const val CollapsedScale = 0.4f
