package com.wpinrui.harmoni.context

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How the phone is moving, as far as activity recognition can tell. */
enum class MotionState { STILL, WALKING, IN_VEHICLE, UNKNOWN }

/**
 * Keeps [state] current from Play Services activity recognition.
 *
 * Updates arrive through a broadcast rather than a callback, because the system delivers them to
 * a PendingIntent, so the latest reading lives here rather than on an instance.
 *
 * [UNKNOWN] until the first update lands, or forever if the permission is refused. Every rule
 * that reads motion treats unknown as "no opinion" rather than guessing at still.
 */
object MotionMonitor {

    private val _state = MutableStateFlow(MotionState.UNKNOWN)
    val state: StateFlow<MotionState> = _state.asStateFlow()

    internal fun publish(state: MotionState) {
        _state.value = state
    }

    fun start(context: Context) {
        if (!context.hasMotionPermission()) {
            Log.i(TAG, "No activity recognition permission, motion stays unknown")
            return
        }

        val intent = Intent(context, MotionReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            // Mutable because the system fills the result into the intent it sends back.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        ActivityRecognition.getClient(context)
            .requestActivityUpdates(UPDATE_INTERVAL_MILLIS, pending)
            .addOnFailureListener { Log.w(TAG, "Activity updates refused", it) }
    }

    private const val UPDATE_INTERVAL_MILLIS = 60_000L
    private const val TAG = "MotionMonitor"
}

/** Receives activity recognition results and hands the most probable one to [MotionMonitor]. */
class MotionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return

        MotionMonitor.publish(
            when (result.mostProbableActivity.type) {
                DetectedActivity.IN_VEHICLE -> MotionState.IN_VEHICLE
                DetectedActivity.ON_BICYCLE -> MotionState.IN_VEHICLE
                DetectedActivity.WALKING, DetectedActivity.ON_FOOT, DetectedActivity.RUNNING ->
                    MotionState.WALKING
                DetectedActivity.STILL -> MotionState.STILL
                else -> MotionState.UNKNOWN
            },
        )
    }
}

fun Context.hasMotionPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED
