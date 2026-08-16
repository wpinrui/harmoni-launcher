package com.wpinrui.harmoni.system

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * A deliberate buzz, rather than one of the framework's stock ticks.
 *
 * `HapticFeedbackType` offers a fixed set of very short effects tuned for acknowledging a press.
 * They are the right weight for the rings, which acknowledge a menu appearing that you are about
 * to choose from, and too light for a gesture that launches something and then hands the screen to
 * another app: by the time the other app is drawing, a tick has been and gone unnoticed.
 *
 * This addresses the vibrator directly so the length and the strength are ours to set.
 */
fun Context.confirmShortcut() {
    val vibrator = getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
    if (!vibrator.hasVibrator()) return

    vibrator.cancel()
    vibrator.vibrate(VibrationEffect.createOneShot(ShortcutMillis, Strength))
}

/** Long enough to register as a buzz rather than a tick, short enough not to feel like an alarm. */
private const val ShortcutMillis = 90L

/**
 * Full amplitude rather than [VibrationEffect.DEFAULT_AMPLITUDE], which is the device's idea of a
 * comfortable notification and is what made this hard to feel.
 */
private const val Strength = 255
