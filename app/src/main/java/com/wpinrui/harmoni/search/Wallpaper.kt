package com.wpinrui.harmoni.search

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

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
fun rememberWallpaper(): State<ImageBitmap?> = WallpaperCache.bitmap.collectAsState()

/**
 * Holds the blurred backdrop's source for the life of the process.
 *
 * Read once at startup rather than when the view opens. Decoding it takes long enough that the
 * entrance animation is over before the bitmap arrives, so the blur would appear fully formed
 * after the fade rather than during it.
 */
object WallpaperCache {

    private val _bitmap = MutableStateFlow<ImageBitmap?>(null)
    val bitmap: StateFlow<ImageBitmap?> = _bitmap.asStateFlow()

    suspend fun prime(context: Context) {
        _bitmap.value = withContext(Dispatchers.IO) { context.readWallpaper() }
    }
}

fun Context.canReadWallpaper(): Boolean = Environment.isExternalStorageManager()

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
