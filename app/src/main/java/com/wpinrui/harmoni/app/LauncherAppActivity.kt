package com.wpinrui.harmoni.app

import androidx.compose.runtime.Composable

/**
 * Harmoni opened as an app rather than as the home surface, per GDD Section 6.
 *
 * This is the entry the app list shows, and the only one: the home activity has no launcher filter
 * because reaching the home surface from a list of apps would be a way of going to where you
 * already are.
 */
class LauncherAppActivity : HarmoniActivity() {

    @Composable
    override fun Content() = LauncherAppScreen()
}
