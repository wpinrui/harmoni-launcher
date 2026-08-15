package com.wpinrui.harmoni

import android.app.Application
import android.content.Context
import com.wpinrui.harmoni.apps.AppIndex
import com.wpinrui.harmoni.apps.IconResolver
import com.wpinrui.harmoni.apps.SystemIconResolver
import com.wpinrui.harmoni.context.MotionMonitor
import com.wpinrui.harmoni.context.UsbConnection

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

    override fun onCreate() {
        super.onCreate()
        appIndex = AppIndex(this).apply { start() }
        iconResolver = SystemIconResolver(this)
        usb = UsbConnection(this).apply { start() }
        MotionMonitor.start(this)
    }
}

/** The shared index and icon resolver, reachable from anywhere holding a [Context]. */
val Context.harmoni: HarmoniApplication get() = applicationContext as HarmoniApplication
