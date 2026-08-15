package com.wpinrui.harmoni.graffiti

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.round

/**
 * The letters the alphabet covers.
 *
 * Letters only. The matcher already strips whitespace from a query, and app names carry digits
 * rarely enough that they are not worth five draws each.
 */
val GraffitiAlphabet: List<Char> = ('a'..'z').toList()

/**
 * How many times each letter is drawn.
 *
 * Every one is kept. The recogniser matches against the nearest template rather than an average,
 * so a sloppy draw is simply a template that never wins, and a letter written two different ways
 * keeps both forms. Averaging would collapse them into a shape that was never drawn.
 */
const val SamplesPerLetter = 5

/** One drawn stroke, in the order the points arrived, in the capture area's pixels. */
data class GraffitiSample(val letter: Char, val points: List<Offset>)

/**
 * Reads and writes the captured alphabet.
 *
 * Points are stored raw rather than resampled, so the recogniser can change how it normalises
 * without needing the whole alphabet drawn again.
 *
 * The file sits in the app's external files directory, which is where `adb pull` can reach it
 * without root. That is how a capture made on the phone becomes the alphabet bundled in the APK.
 */
object GraffitiStore {

    private const val TAG = "GraffitiStore"
    private const val FILE_NAME = "graffiti.json"
    private const val VERSION = 1

    fun file(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, FILE_NAME)

    fun load(context: Context): List<GraffitiSample> {
        val file = file(context)
        if (!file.exists()) return emptyList()

        return runCatching { parse(file.readText()) }
            .onFailure { Log.w(TAG, "Cannot read ${file.path}", it) }
            .getOrDefault(emptyList())
    }

    /**
     * Written to a temporary file and renamed, so a process death mid-write leaves the previous
     * capture intact rather than a truncated one. An alphabet is a couple of hundred draws and
     * losing it to a half-written file would be miserable.
     */
    fun save(context: Context, samples: List<GraffitiSample>) {
        val file = file(context)
        val scratch = File(file.parentFile, "$FILE_NAME.tmp")

        runCatching {
            scratch.writeText(serialise(samples))
            if (!scratch.renameTo(file)) {
                file.writeText(scratch.readText())
                scratch.delete()
            }
        }.onFailure { Log.w(TAG, "Cannot write ${file.path}", it) }
    }

    private fun serialise(samples: List<GraffitiSample>): String {
        val array = JSONArray()
        samples.forEach { sample ->
            val points = JSONArray()
            sample.points.forEach { point ->
                points.put(JSONArray().put(oneDecimal(point.x)).put(oneDecimal(point.y)))
            }
            array.put(
                JSONObject()
                    .put("letter", sample.letter.toString())
                    .put("points", points),
            )
        }

        return JSONObject()
            .put("version", VERSION)
            .put("samples", array)
            .toString()
    }

    private fun parse(text: String): List<GraffitiSample> {
        val samples = JSONObject(text).getJSONArray("samples")

        return (0 until samples.length()).mapNotNull { index ->
            val sample = samples.getJSONObject(index)
            val letter = sample.getString("letter").firstOrNull() ?: return@mapNotNull null
            val points = sample.getJSONArray("points")

            GraffitiSample(
                letter = letter,
                points = (0 until points.length()).map { at ->
                    val point = points.getJSONArray(at)
                    Offset(point.getDouble(0).toFloat(), point.getDouble(1).toFloat())
                },
            )
        }
    }

    /** Full float precision would triple the file for detail no finger produces. */
    private fun oneDecimal(value: Float) = round(value * 10f) / 10.0
}
