package com.wpinrui.harmoni.app

import android.util.Log
import kotlin.system.exitProcess

/**
 * Restarts Harmoni after a settings change.
 *
 * Held until the home surface next comes to the front, which is the one moment where a restart
 * costs nothing: the system brings the home app straight back, and nothing is on screen to lose.
 * Firing on the change itself would tear down the settings screen mid-edit, and a run of changes
 * settles into one restart rather than one each.
 *
 * The flag lives in memory only, so the restart clears it by definition.
 */
object SettingsRestart {

    private const val TAG = "SettingsRestart"

    @Volatile
    private var pending = false

    fun mark() {
        pending = true
    }

    fun applyIfPending() {
        if (!pending) return
        pending = false
        Log.i(TAG, "Settings changed, restarting")
        exitProcess(0)
    }
}
