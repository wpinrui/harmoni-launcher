package com.wpinrui.harmoni.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colours every screen shares.
 *
 * Everything Harmoni draws over the wallpaper is built from these, so they live here rather
 * than being retyped as a hex literal per screen.
 */

/** The ground behind anything that is not the wallpaper, and the base of the search view's tint. */
val Ground = Color(0xFF120E0C)

/** Anything the eye is meant to go to first: a section title, an action, the start of a stroke. */
val Accent = Color(0xFFE8B979)

/** Labels and counts that sit beside the thing they describe rather than being it. */
val Meta = Color(0xFFCFC6BD)
