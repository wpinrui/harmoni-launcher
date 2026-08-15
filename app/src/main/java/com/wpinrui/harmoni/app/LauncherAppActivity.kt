package com.wpinrui.harmoni.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wpinrui.harmoni.ui.theme.HarmoniTheme

/**
 * Harmoni opened as an app rather than as the home surface, per Section 5.
 *
 * This is the entry the app list shows, and the only one: the home activity has no launcher filter
 * because reaching the home surface from a list of apps would be a way of going to where you
 * already are.
 */
class LauncherAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            HarmoniTheme {
                LauncherAppScreen()
            }
        }
    }
}
