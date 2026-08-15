package com.wpinrui.harmoni.app

import androidx.compose.runtime.Composable

/** The hidden apps list, opened from the launcher app screen. */
class HiddenAppsActivity : HarmoniActivity() {

    @Composable
    override fun Content() = HiddenAppsScreen()
}
