package com.wpinrui.harmoni.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The empty surface the rest of the launcher is built on.
 *
 * It fills the window and paints nothing, so the wallpaper is the background. Section 1's clock
 * block goes inside it and Section 3's gesture layer wraps it. Both need it to stay transparent:
 * an opaque background here would hide the wallpaper the whole design rests on.
 */
@Composable
fun HomeSurface(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}
