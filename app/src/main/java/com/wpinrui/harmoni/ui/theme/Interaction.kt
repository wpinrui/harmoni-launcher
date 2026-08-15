package com.wpinrui.harmoni.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Tappable without a ripple.
 *
 * Every surface in the launcher sits over a wallpaper or a blurred copy of one, and a ripple
 * smears across both.
 *
 * Not enabled means the modifier is not applied at all, rather than applied and switched off. A
 * disabled `clickable` still installs its gesture node, and that node consumes the press even
 * though it does nothing with it, which silently swallows anything aimed at what lies beneath.
 * That distinction is load-bearing for the ring, where icons in flight sit over the centre.
 */
@Composable
fun Modifier.noRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    if (!enabled) return this
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
