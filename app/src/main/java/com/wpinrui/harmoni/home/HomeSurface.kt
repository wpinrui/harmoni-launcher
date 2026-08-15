package com.wpinrui.harmoni.home

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.context.ContextualRing
import com.wpinrui.harmoni.diagnostics.Diagnostics
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalViewConfiguration
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.search.SearchSurface
import com.wpinrui.harmoni.shortcuts.GestureBindings
import com.wpinrui.harmoni.shortcuts.ShortcutGesture
import com.wpinrui.harmoni.system.NotificationShade

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
    var ringCentre by remember { mutableStateOf<Offset?>(null) }
    var slots by remember { mutableStateOf(emptyList<RingTarget?>()) }
    var ringInteractive by remember { mutableStateOf(true) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val doubleTapWindow = LocalViewConfiguration.current.doubleTapTimeoutMillis
    val context = LocalContext.current
    val contextual = remember(context) { ContextualRing(context) }
    val alphabet by context.harmoni.graffiti.collectAsState()
    val haptics = LocalHapticFeedback.current
    val swipeTravel = with(density) { SwipeTravel.toPx() }

    val entries by context.harmoni.appIndex.entries.collectAsState()
    val overrides by RingSlots.overrides.collectAsState()
    val hidden by HiddenApps.packages.collectAsState()
    val fixed = remember(overrides, entries, hidden) { ringTargets(overrides, entries, hidden) }

    // Coming home while the ring or the app list is up means "get me back to the wallpaper".
    LaunchedEffect(Unit) {
        HomePresses.presses.collect {
            searchOpen = false
            ringCentre = null
        }
    }

    LaunchedEffect(ringCentre) {
        if (ringCentre != null && !ringInteractive) {
            delay(doubleTapWindow)
            ringInteractive = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { surface = Size(it.width.toFloat(), it.height.toFloat()) }
            .homeGestures { gesture ->
                when (gesture) {
                    is HomeGesture.Tap -> {
                        slots = fixed
                        // Deaf until the moment a second tap could no longer arrive, so the
                        // surface keeps both halves of a double tap.
                        ringInteractive = false
                        ringCentre = RingPlacement.clamp(gesture.position, surface, density)
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }

                    is HomeGesture.DoubleTap -> {
                        ringCentre = null
                        searchQuery = ""
                        searchOpen = true
                    }

                    is HomeGesture.LongPress -> {
                        slots = contextual.slots()
                        // A long press cannot be half a double tap, so this one is live at once.
                        ringInteractive = true
                        ringCentre = RingPlacement.clamp(gesture.position, surface, density)
                        // Heavier than the tap ring's: the finger is still down and this is
                        // the only signal that waiting has paid off.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    is HomeGesture.SwipeUp ->
                        GestureBindings.start(context, ShortcutGesture.SWIPE_UP)

                    is HomeGesture.TwoFingerSwipeUp ->
                        GestureBindings.start(context, ShortcutGesture.TWO_FINGER_SWIPE_UP)

                    // Section 4 opens the search view on the first stroke, carrying that letter.
                    // A stroke that matches nothing is left alone: opening an empty search would
                    // put the whole surface behind a scrim for what was probably a stray swipe.
                    is HomeGesture.Stroke -> {
                        val start = gesture.points.firstOrNull()

                        when {
                            start == null -> Unit

                            // Above the writing area, a straight run downward is the shade. Shape
                            // alone cannot tell it from the letter i, which is also a straight
                            // vertical; where it started is what separates them.
                            start.y < surface.height * GraffitiTop ->
                                if (isStraightSwipe(gesture.points, swipeTravel, downward = true)) {
                                    NotificationShade.expand(context)
                                } else {
                                    Log.d(TAG, "Stroke above the input area, ignored")
                                }

                            else -> {
                                val match = alphabet.recognise(gesture.points)
                                if (match == null) {
                                    Log.d(TAG, "Stroke of ${gesture.points.size} points matched no letter")
                                } else {
                                    ringCentre = null
                                    searchQuery = match.letter.toString()
                                    searchOpen = true
                                }
                            }
                        }
                    }
                }
            },
    ) {
        ClockBlock()

        ringCentre?.let { centre ->
            Ring(
                centre = centre,
                slots = slots,
                onPick = { target ->
                    ringCentre = null
                    context.launch(target)
                },
                onDismiss = {
                    Log.d(TAG, "Ring dismissed without a pick")
                    Diagnostics.recordRingDismissal(context)
                    ringCentre = null
                },
                interactive = ringInteractive,
            )
        }

        if (searchOpen) {
            SearchSurface(
                initialQuery = searchQuery,
                onLaunch = { entry ->
                    searchOpen = false
                    context.launchApp(entry.packageName)
                },
                onClose = { searchOpen = false },
            )
        }
    }
}

private const val TAG = "HomeSurface"

/**
 * Where the Graffiti area starts, as a fraction of the surface.
 *
 * The same place the search view puts it: measured down that layout, the query line, the result
 * count, the 4x2 grid and the page dots come to just under half the height, so a stroke drawn on
 * the bare wallpaper registers exactly where it would once the view is open. Nothing marks it,
 * because nothing marks it there either.
 *
 * Only the stroke's first point is tested. A letter that runs up out of the area is still a letter;
 * one that starts above it was never aimed at the input.
 */
private const val GraffitiTop = 0.5f
