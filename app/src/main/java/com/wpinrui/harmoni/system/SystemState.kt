package com.wpinrui.harmoni.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime

/**
 * Clock and battery, both driven by system broadcasts rather than a poll.
 *
 * ACTION_TIME_TICK fires on the minute, which is exactly the resolution the clock shows, and
 * battery changes arrive as they happen. Neither is delivered while the launcher is stopped, so
 * both re-read on the way back in.
 */

/** The current time, updated on the minute and whenever the clock or time zone is changed. */
@Composable
fun rememberClock(): State<LocalDateTime> {
    val context = LocalContext.current
    val time = remember { mutableStateOf(LocalDateTime.now()) }

    SystemBroadcast(
        Intent.ACTION_TIME_TICK,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        onReceive = { time.value = LocalDateTime.now() },
    )

    // ACTION_TIME_TICK is not sticky, so the first minute after a resume would otherwise show a
    // stale time.
    remember(context) { time.value = LocalDateTime.now() }
    return time
}

/** Battery charge as a percentage. */
@Composable
fun rememberBatteryPercent(): State<Int> {
    val context = LocalContext.current
    val percent = remember { mutableIntStateOf(readBatteryPercent(context)) }

    SystemBroadcast(Intent.ACTION_BATTERY_CHANGED) { intent ->
        percent.intValue = intent.batteryPercent() ?: percent.intValue
    }

    return percent
}

/** True when the device is set to 24 hour time. */
@Composable
fun rememberIs24Hour(): Boolean {
    val context = LocalContext.current
    var is24Hour by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }

    SystemBroadcast(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED) {
        is24Hour = DateFormat.is24HourFormat(context)
    }

    return is24Hour
}

/** Registers a receiver for [actions] for as long as the caller is composed. */
@Composable
private fun SystemBroadcast(vararg actions: String, onReceive: (Intent) -> Unit) {
    val context = LocalContext.current
    val currentOnReceive by rememberUpdatedState(onReceive)
    val filter = remember(actions) { IntentFilter().apply { actions.forEach(::addAction) } }

    androidx.compose.runtime.DisposableEffect(context, filter) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = currentOnReceive(intent)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
}

private fun readBatteryPercent(context: Context): Int =
    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?.batteryPercent()
        ?: 0

private fun Intent.batteryPercent(): Int? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return level * 100 / scale
}
