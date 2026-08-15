package com.wpinrui.harmoni.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Typography and colour defaults only.
 *
 * Background and surface are transparent on purpose: everything Harmoni draws sits over the
 * wallpaper, so any component that fills its background with the scheme colour disappears
 * instead of covering it.
 */
private val HarmoniColorScheme = darkColorScheme(
    background = Color.Transparent,
    surface = Color.Transparent,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun HarmoniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HarmoniColorScheme,
        content = content,
    )
}
