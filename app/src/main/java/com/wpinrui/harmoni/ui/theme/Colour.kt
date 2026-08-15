package com.wpinrui.harmoni.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The two colours every screen shares.
 *
 * Everything Harmoni draws on top of the wallpaper is either this near-black ground or this warm
 * accent, so both live here rather than being retyped as a hex literal per screen.
 */

/** The ground behind anything that is not the wallpaper, and the base of the search view's tint. */
val Ground = Color(0xFF120E0C)

/** Anything the eye is meant to go to first: a section title, an action, the start of a stroke. */
val Accent = Color(0xFFE8B979)
