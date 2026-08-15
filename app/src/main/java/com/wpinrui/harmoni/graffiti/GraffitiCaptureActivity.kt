package com.wpinrui.harmoni.graffiti

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.ui.theme.HarmoniTheme

/**
 * Records the Graffiti alphabet.
 *
 * Opened as an ordinary app rather than from the home surface, which is why it carries a LAUNCHER
 * filter: Harmoni's own all apps grid lists it, so it is reached by double tapping home and typing
 * its name. The capture is a one-off per device, then the file is pulled and bundled.
 */
class GraffitiCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            HarmoniTheme {
                GraffitiCaptureScreen()
            }
        }
    }

    /**
     * On the way out rather than after each stroke: parsing the whole alphabet 130 times during a
     * capture would be wasted work, and nothing reads the templates while this screen is up.
     */
    override fun onStop() {
        super.onStop()
        harmoni.reloadGraffiti()
    }
}
