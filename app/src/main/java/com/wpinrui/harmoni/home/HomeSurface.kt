package com.wpinrui.harmoni.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.context.ContextualRing
import com.wpinrui.harmoni.diagnostics.Diagnostics
import com.wpinrui.harmoni.graffiti.GraffitiAlphabet
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.search.SearchSurface
import com.wpinrui.harmoni.shortcuts.GestureBindings
import com.wpinrui.harmoni.shortcuts.ShortcutGesture
import com.wpinrui.harmoni.system.NotificationShade
import com.wpinrui.harmoni.system.buzzShortcutStarted
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * The surface the rest of the launcher is built on.
 *
 * It fills the window and paints nothing but the clock block, so the wallpaper is the background,
 * and it classifies every touch the block does not take. A tap summons the fixed ring, a long
 * press the contextual one, a double tap the all apps view, a letter the search view, a swipe up
 * whatever shortcut is bound to it, and a swipe down the notification shade.
 */
@Composable
fun HomeSurface(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val doubleTapWindow = LocalViewConfiguration.current.doubleTapTimeoutMillis

    var surface by remember { mutableStateOf(Size.Zero) }
    var ringCentre by remember { mutableStateOf<Offset?>(null) }
    var slots by remember { mutableStateOf(emptyList<RingTarget?>()) }
    var ringInteractive by remember { mutableStateOf(true) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val contextual = remember(context) { ContextualRing(context) }

    // Four months of usage stats, too slow to read with a finger down. Coming back to the home
    // surface is the moment before any press, so that is where it is refreshed.
    LifecycleResumeEffect(contextual) {
        contextual.refresh()
        onPauseOrDispose {}
    }
    val alphabet by context.harmoni.graffiti.collectAsState()
    val entries by context.harmoni.appIndex.entries.collectAsState()
    val overrides by RingSlots.overrides.collectAsState()
    val hidden by HiddenApps.packages.collectAsState()
    val fixed = remember(overrides, entries, hidden) { ringTargets(overrides, entries, hidden) }

    // The gesture handler is installed once and never relaunched, so anything it reads has to be
    // wrapped rather than captured. Without this the tap ring would keep serving whatever the
    // bindings were at the first composition.
    val state by rememberUpdatedState(
        SurfaceState(
            surface = surface,
            fixed = fixed,
            alphabet = alphabet,
            density = density,
            haptics = haptics,
        ),
    )

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
                handle(
                    gesture = gesture,
                    state = state,
                    context = context,
                    contextual = contextual,
                    onRing = { centre, targets, interactive ->
                        slots = targets
                        ringInteractive = interactive
                        ringCentre = centre
                    },
                    onSearch = { query ->
                        ringCentre = null
                        searchQuery = query
                        searchOpen = true
                    },
                )
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
                    context.launch(entry)
                },
                onClose = { searchOpen = false },
            )
        }
    }
}

/** Everything the gesture handler reads from composition, in one value it can be handed. */
private data class SurfaceState(
    val surface: Size,
    val fixed: List<RingTarget?>,
    val alphabet: GraffitiAlphabet,
    val density: Density,
    val haptics: HapticFeedback,
)

private fun handle(
    gesture: HomeGesture,
    state: SurfaceState,
    context: Context,
    contextual: ContextualRing,
    onRing: (Offset, List<RingTarget?>, Boolean) -> Unit,
    onSearch: (String) -> Unit,
) {
    when (gesture) {
        is HomeGesture.Tap -> {
            // Deaf until the moment a second tap could no longer arrive, so the surface keeps
            // both halves of a double tap.
            onRing(state.centre(gesture.position), state.fixed, false)
            state.haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }

        is HomeGesture.LongPress -> {
            // A long press cannot be half a double tap, so this one is live at once. The heavier
            // haptic is the only signal that waiting has paid off, with the finger still down.
            onRing(state.centre(gesture.position), contextual.slots(), true)
            state.haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        is HomeGesture.DoubleTap -> onSearch("")

        is HomeGesture.SwipeUp -> runShortcut(context, ShortcutGesture.SWIPE_UP)

        is HomeGesture.TwoFingerSwipeUp ->
            runShortcut(context, ShortcutGesture.TWO_FINGER_SWIPE_UP)

        is HomeGesture.Stroke -> handleStroke(gesture.points, state, context, onSearch)
    }
}

/**
 * A stroke is a letter, the notification shade, or nothing, and where it starts decides which.
 *
 * Shape alone cannot separate a swipe down from the letter i, which is a straight vertical at 0.99
 * straightness. The writing area is the lower half, so above it a straight run downward is the
 * shade and below it everything goes to the recogniser.
 *
 * A stroke that matches no letter is left alone: opening an empty search would put the whole
 * surface behind a scrim for what was probably a stray swipe.
 */
private fun handleStroke(
    points: List<Offset>,
    state: SurfaceState,
    context: Context,
    onSearch: (String) -> Unit,
) {
    val start = points.firstOrNull() ?: return
    val travel = with(state.density) { SwipeTravel.toPx() }

    if (start.y < state.surface.height * GraffitiTop) {
        if (isStraightSwipe(points, travel, downward = true)) {
            NotificationShade.expand(context)
        } else {
            Log.d(TAG, "Stroke above the writing area, ignored")
        }
        return
    }

    val match = state.alphabet.recognise(points)
    if (match == null) {
        Log.d(TAG, "Stroke of ${points.size} points matched no letter")
        return
    }

    onSearch(match.letter.toString())
}

/**
 * Runs whatever is bound to [gesture], and confirms it in the hand.
 *
 * Only when something actually started. A swipe up that is bound to nothing, or to a shortcut
 * the app has since withdrawn, leaves the screen exactly as it was, and a buzz there would be
 * telling the finger something happened when nothing did.
 */
private fun runShortcut(context: Context, gesture: ShortcutGesture) {
    if (GestureBindings.start(context, gesture)) context.buzzShortcutStarted()
}

private fun SurfaceState.centre(position: Offset) = RingPlacement.clamp(position, surface, density)

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
