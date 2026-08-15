package com.wpinrui.harmoni

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wpinrui.harmoni.context.MotionMonitor
import com.wpinrui.harmoni.context.hasMotionPermission
import com.wpinrui.harmoni.home.HomePresses
import com.wpinrui.harmoni.home.HomeSurface
import com.wpinrui.harmoni.ui.theme.HarmoniTheme

/**
 * The home surface, registered for CATEGORY_HOME.
 *
 * Nothing is drawn here yet beyond the wallpaper showing through. Section 1's clock block and
 * Section 3's gesture layer both attach to [HomeSurface].
 */
class HomeActivity : ComponentActivity() {

    // Motion feeds the contextual ring's transit rules. Refusing it costs those rules and
    // nothing else, so it is asked for once and never nagged about.
    private val requestMotion =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) MotionMonitor.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars, light icons: the wallpaper is what shows behind them, and a scrim
        // would put a band across the top and bottom of it.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // The block already shows the time and the battery, so the status bar would only repeat
        // them over the wallpaper. It stays available on a swipe from the top edge.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            HarmoniTheme {
                HomeSurface()
            }
        }

        // Home is the bottom of the stack. Finishing it would expose whatever sits behind the
        // launcher, so back does nothing.
        if (!hasMotionPermission()) {
            requestMotion.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
        }

        onBackPressedDispatcher.addCallback(this) {}
    }

    /**
     * Pressing or swiping home while Harmoni is already the top activity arrives here, since the
     * activity is never recreated. It is the only signal that the gesture happened.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        HomePresses.record()
    }
}
