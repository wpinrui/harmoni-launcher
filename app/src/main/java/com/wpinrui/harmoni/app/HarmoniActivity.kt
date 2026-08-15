package com.wpinrui.harmoni.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.wpinrui.harmoni.ui.theme.HarmoniTheme

/**
 * An ordinary Harmoni screen: edge to edge, transparent bars, the Harmoni theme.
 *
 * The home surface is not one of these. It hosts the wallpaper and has its own window flags.
 */
abstract class HarmoniActivity : ComponentActivity() {

    @Composable
    protected abstract fun Content()

    final override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent { HarmoniTheme { Content() } }
    }
}
