package com.wpinrui.harmoni

import android.app.Application
import android.content.Context
import com.wpinrui.harmoni.apps.AppIndex
import com.wpinrui.harmoni.apps.IconResolver
import com.wpinrui.harmoni.apps.SystemIconResolver
import com.wpinrui.harmoni.context.MotionMonitor
import com.wpinrui.harmoni.context.UsbConnection
import com.wpinrui.harmoni.diagnostics.Diagnostics
import com.wpinrui.harmoni.graffiti.GraffitiAlphabet
import com.wpinrui.harmoni.search.WallpaperCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-level home for the pieces every surface shares.
 *
 * The index and the resolver live for the life of the process rather than the activity: the home
 * activity is destroyed and rebuilt on things like a display change, and rebuilding the index
 * with it would mean a cold app list each time.
 */
class HarmoniApplication : Application() {

    lateinit var appIndex: AppIndex
        private set

    lateinit var iconResolver: IconResolver
        private set

    /** Watched for the whole process, since the contextual ring reads it at the moment of a press. */
    lateinit var usb: UsbConnection
        private set

    private val _graffiti = MutableStateFlow(GraffitiAlphabet(emptyList()))

    /**
     * The stroke templates, empty until they have been parsed.
     *
     * Loaded off the main thread: it is a few hundred kilobytes of JSON, and holding up the first
     * frame of the home surface for it would trade a visible stall for a stroke nobody is drawing
     * in the first moments after boot anyway.
     */
    val graffiti: StateFlow<GraffitiAlphabet> = _graffiti.asStateFlow()

    /**
     * Picks up letters redrawn since the process started.
     *
     * The launcher process outlives every other screen, so without this a recapture would not take
     * effect until something killed it, which for the home app is nothing short of a reboot.
     */
    fun reloadGraffiti() {
        CoroutineScope(Dispatchers.Default).launch { _graffiti.value = GraffitiAlphabet.load(this@HarmoniApplication) }
    }

    override fun onCreate() {
        super.onCreate()
        appIndex = AppIndex(this).apply { start() }
        iconResolver = SystemIconResolver(this)
        usb = UsbConnection(this).apply { start() }
        MotionMonitor.start(this)
        Diagnostics.load(this)

        CoroutineScope(Dispatchers.Default).launch {
            // Decoded up front so the search view's backdrop is ready to fade in with everything
            // else rather than arriving after the animation has finished.
            WallpaperCache.prime(this@HarmoniApplication)
            _graffiti.value = GraffitiAlphabet.load(this@HarmoniApplication)
        }
    }
}

/** The shared index and icon resolver, reachable from anywhere holding a [Context]. */
val Context.harmoni: HarmoniApplication get() = applicationContext as HarmoniApplication
