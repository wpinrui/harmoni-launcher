package com.wpinrui.harmoni.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.wpinrui.harmoni.HarmoniApplication
import com.wpinrui.harmoni.harmoni

/**
 * An installed app's icon, drawn at [size].
 *
 * Nothing is drawn when the app is missing, so an unbound slot leaves a gap rather than a
 * placeholder. Icons come from [HarmoniApplication.iconResolver], which is where icon pack
 * support will appear.
 */
@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val app = LocalContext.current.harmoni
    val entries by app.appIndex.entries.collectAsState()
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val bitmap: ImageBitmap? = remember(packageName, entries, sizePx) {
        app.appIndex.firstFor(packageName)
            ?.let { app.iconResolver.iconFor(it) }
            ?.toBitmap(width = sizePx, height = sizePx)
            ?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    }
}
