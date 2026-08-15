package com.wpinrui.harmoni.home

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

/**
 * The surface the rest of the launcher is built on.
 *
 * It fills the window and paints nothing, so the wallpaper is the background, and it classifies
 * every touch that the clock block does not take. The ring and Graffiti do not exist yet, so for
 * now a classified gesture is logged and, when a tap lands too near an edge, flashed.
 */
@Composable
fun HomeSurface(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var surface by remember { mutableStateOf(Size.Zero) }
    var rejectCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { surface = Size(it.width.toFloat(), it.height.toFloat()) }
            .homeGestures { gesture ->
                when (gesture) {
                    is HomeGesture.Tap ->
                        if (RingPlacement.fits(gesture.position, surface, density)) {
                            Log.d(TAG, "Ring at ${gesture.position}")
                        } else {
                            rejectCount++
                            Log.d(TAG, "Tap rejected, too near an edge: ${gesture.position}")
                        }

                    is HomeGesture.LongPress ->
                        if (RingPlacement.fits(gesture.position, surface, density)) {
                            Log.d(TAG, "Contextual ring at ${gesture.position}")
                        } else {
                            rejectCount++
                            Log.d(TAG, "Long press rejected, too near an edge: ${gesture.position}")
                        }

                    is HomeGesture.Stroke ->
                        Log.d(TAG, "Stroke of ${gesture.points.size} points")
                }
            },
    ) {
        ClockBlock()
        EdgeRejectFlash(rejectCount = rejectCount)
    }
}

private const val TAG = "HomeSurface"
