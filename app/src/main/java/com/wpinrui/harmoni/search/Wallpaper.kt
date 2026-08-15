package com.wpinrui.harmoni.search

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The wallpaper as a bitmap, for drawing a blurred copy of it inside the launcher's own window.
 *
 * The window manager can only blur what sits behind a window, and the launcher's window is where
 * the wallpaper is drawn, so there is nothing behind it to blur. Doing it ourselves also means the
 * blur is ordinary Compose drawing, which animates like everything else.
 *
 * Reading it needs all-files access. Image permission is not enough: the platform checks storage
 * access for a wallpaper read even when the image permission is granted. Without it, or with a
 * live wallpaper, this is null and the search view falls back to its tint alone.
 */
@Composable
fun rememberWallpaper(): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { context.readWallpaper() }
    }
}

fun Context.canReadWallpaper(): Boolean = Environment.isExternalStorageManager()

/** Takes the user to the all-files toggle, which is a Settings screen rather than a prompt. */
fun Context.requestWallpaperAccess() {
    startActivity(
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:$packageName".toUri(),
        ),
    )
}

private fun Context.readWallpaper(): ImageBitmap? {
    if (!canReadWallpaper()) {
        Log.i(TAG, "No all-files access, so no wallpaper to blur")
        return null
    }

    return try {
        val drawable = getSystemService(WallpaperManager::class.java)
            ?.getDrawable(WallpaperManager.FLAG_SYSTEM)
        if (drawable == null) {
            Log.i(TAG, "Wallpaper is not readable, probably a live one")
            return null
        }

        // Shrunk hard before it is blurred. A blur averages neighbours, so doing it at a fraction
        // of the size is both much cheaper and, scaled back up, softer.
        val full = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        val height = (full.height * (SampleWidth.toFloat() / full.width)).toInt().coerceAtLeast(1)
        full.scale(SampleWidth, height).asImageBitmap()
    } catch (e: SecurityException) {
        Log.w(TAG, "Refused the wallpaper", e)
        null
    }
}

private const val SampleWidth = 320
private const val TAG = "Wallpaper"
