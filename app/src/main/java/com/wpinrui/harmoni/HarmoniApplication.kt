package com.wpinrui.harmoni

import android.app.Application
import com.wpinrui.harmoni.apps.AppIndex
import com.wpinrui.harmoni.apps.IconResolver
import com.wpinrui.harmoni.apps.SystemIconResolver

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

    override fun onCreate() {
        super.onCreate()
        appIndex = AppIndex(this).apply { start() }
        iconResolver = SystemIconResolver(this)
    }
}
