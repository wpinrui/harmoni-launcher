package com.wpinrui.harmoni.graffiti

import androidx.compose.runtime.Composable
import com.wpinrui.harmoni.app.HarmoniActivity
import com.wpinrui.harmoni.harmoni

/**
 * Records the Graffiti alphabet.
 *
 * Reached from the launcher app screen rather than from the app list, since a capture is a one-off
 * per device and a second Harmoni entry would sit in the grid forever to serve it.
 */
class GraffitiCaptureActivity : HarmoniActivity() {

    @Composable
    override fun Content() = GraffitiCaptureScreen()

    /**
     * On the way out rather than after each stroke: parsing the whole alphabet 130 times during a
     * capture would be wasted work, and nothing reads the templates while this screen is up.
     */
    override fun onStop() {
        super.onStop()
        harmoni.reloadGraffiti()
    }
}
