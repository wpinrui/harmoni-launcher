package com.wpinrui.harmoni.graffiti

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset

/**
 * The templates every stroke is matched against.
 *
 * Built from the alphabet bundled in the APK, with any letter recaptured on this device replacing
 * the bundled samples for that letter alone. Redrawing a q that never quite worked therefore costs
 * five strokes rather than the whole alphabet, and a half-finished capture cannot leave the other
 * twenty-five letters unrecognisable.
 */
class GraffitiAlphabet(private val templates: List<GraffitiTemplate>) {

    fun recognise(points: List<Offset>): GraffitiMatch? =
        GraffitiRecogniser.recognise(points, templates)

    companion object {

        private const val TAG = "GraffitiAlphabet"

        fun load(context: Context): GraffitiAlphabet {
            val bundled = GraffitiStore.bundled(context).groupBy { it.letter }
            val captured = GraffitiStore.load(context).groupBy { it.letter }
            val samples = (bundled + captured).values.flatten()

            val templates = samples.mapNotNull { sample ->
                GraffitiRecogniser.prepare(sample.points)?.let { GraffitiTemplate(sample.letter, it) }
            }

            Log.i(TAG, "Loaded ${templates.size} templates over ${(bundled + captured).size} letters")
            return GraffitiAlphabet(templates)
        }
    }
}
