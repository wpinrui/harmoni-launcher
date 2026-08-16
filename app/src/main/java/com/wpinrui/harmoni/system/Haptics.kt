package com.wpinrui.harmoni.system

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * A deliberate buzz, rather than one of the framework's stock effects.
 *
 * `HapticFeedbackType` offers a fixed set of very short effects. They are the right weight for the
 * rings, which acknowledge a menu appearing that you are about to choose from, and too light for a
 * gesture that launches something and then hands the screen to another app: measured on the
 * device, `Confirm` had been and gone before the launched app finished drawing over it.
 *
 * This addresses the vibrator directly so the length is ours to set. Declaring the usage as touch
 * keeps it tied to the same system setting the rings obey, so turning haptics off silences all
 * three; the cost is that the device scales the amplitude, which is why the length rather than the
 * strength is what carries it.
 */
fun Context.buzzShortcutStarted() {
    val vibrator = getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
    if (!vibrator.hasVibrator()) return

    // Two gestures inside the window should read as two buzzes rather than one long one.
    vibrator.cancel()
    vibrator.vibrate(
        VibrationEffect.createOneShot(ShortcutMillis, ShortcutAmplitude),
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH),
    )
}

/** Long enough to register as a buzz rather than a tick, short enough not to feel like an alarm. */
private const val ShortcutMillis = 90L

/** The top of the 1..255 range, leaving the device's own scaling as the only thing damping it. */
private const val ShortcutAmplitude = 255
