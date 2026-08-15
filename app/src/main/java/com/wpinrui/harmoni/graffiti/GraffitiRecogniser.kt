package com.wpinrui.harmoni.graffiti

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** A captured sample reduced to the form matching compares, [ResampleCount] points long. */
data class GraffitiTemplate(val letter: Char, val points: List<Offset>)

/** The winning letter and how well it fit, on a scale where 1 is an identical stroke. */
data class GraffitiMatch(val letter: Char, val score: Float)

/**
 * Matches a stroke against the captured alphabet, after Wobbrock, Wilson and Li's $1 recogniser.
 *
 * The stroke is resampled to a fixed number of evenly spaced points, scaled to a reference size and
 * centred, then compared point for point against every template. Nearest wins. There is no training
 * step and no averaging: five samples of a letter are five templates, so a sloppy one is simply a
 * template that never wins and a letter written two ways keeps both forms.
 *
 * Two deliberate departures from the paper:
 *
 * Scaling is uniform rather than to a square. $1 stretches each axis independently, which is fine
 * for gestures but ruins an alphabet: a straight vertical has no width, so squaring it up magnifies
 * the finger's jitter into the entire shape.
 *
 * There is no rotation normalisation. $1's indicative-angle step makes matching rotation invariant,
 * which for letters means M matches W and N matches Z. Measured on the captured alphabet, dropping
 * it took leave-one-out accuracy from 98.5% to 130 of 130.
 */
object GraffitiRecogniser {

    private const val ResampleCount = 64
    private const val ReferenceSize = 250f

    /** The furthest apart two normalised strokes can be, which turns a distance into a score. */
    private val WidestDistance = hypot(ReferenceSize, ReferenceSize) / 2f

    /**
     * Null when the stroke is too short to have a shape, or nothing clears [floor].
     *
     * Genuine letters in the captured alphabet matched their own siblings at 0.785 and above, so a
     * floor below that rejects strays without turning away real handwriting.
     */
    fun recognise(
        points: List<Offset>,
        templates: List<GraffitiTemplate>,
        floor: Float = MatchFloor,
    ): GraffitiMatch? {
        if (templates.isEmpty()) return null
        val candidate = prepare(points) ?: return null

        var best: GraffitiTemplate? = null
        var shortest = Float.MAX_VALUE

        templates.forEach { template ->
            val distance = distance(candidate, template.points)
            if (distance < shortest) {
                shortest = distance
                best = template
            }
        }

        val winner = best ?: return null
        val score = 1f - shortest / WidestDistance
        return if (score >= floor) GraffitiMatch(winner.letter, score) else null
    }

    /** Resampled and normalised, or null if the stroke has no length to resample along. */
    fun prepare(points: List<Offset>): List<Offset>? {
        if (points.size < 2) return null
        val resampled = resample(points) ?: return null
        return normalise(resampled)
    }

    /**
     * Evenly spaced points along the path, so how fast the letter was drawn stops mattering.
     *
     * Walks the path accumulating length and inserts a point every interval, splitting whichever
     * segment the interval lands in rather than rounding to the nearest captured point.
     */
    private fun resample(points: List<Offset>): List<Offset>? {
        val length = points.zipWithNext().fold(0f) { total, (a, b) -> total + (b - a).getDistance() }
        val interval = length / (ResampleCount - 1)
        if (interval <= 0f) return null

        val out = mutableListOf(points.first())
        var accumulated = 0f
        var previous = points.first()
        var index = 1

        while (index < points.size) {
            val next = points[index]
            val segment = (next - previous).getDistance()

            if (accumulated + segment >= interval) {
                val ratio = (interval - accumulated) / segment
                val split = Offset(
                    x = previous.x + ratio * (next.x - previous.x),
                    y = previous.y + ratio * (next.y - previous.y),
                )
                out += split
                // Carry on from the split rather than from the captured point, so the next
                // interval is measured from where this one actually ended.
                previous = split
                accumulated = 0f
            } else {
                accumulated += segment
                previous = next
                index++
            }
        }

        // Rounding can leave the last point or two unplaced on a path that ends exactly on an
        // interval boundary.
        while (out.size < ResampleCount) out += points.last()
        return out.take(ResampleCount)
    }

    private fun normalise(points: List<Offset>): List<Offset> {
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val width = points.maxOf { it.x } - minX
        val height = points.maxOf { it.y } - minY

        val scale = ReferenceSize / max(max(width, height), 0.001f)
        val scaled = points.map { Offset(it.x * scale, it.y * scale) }

        var sumX = 0f
        var sumY = 0f
        scaled.forEach { sumX += it.x; sumY += it.y }
        val centre = Offset(sumX / scaled.size, sumY / scaled.size)

        return scaled.map { it - centre }
    }

    private fun distance(a: List<Offset>, b: List<Offset>): Float {
        var total = 0f
        for (index in a.indices) total += (a[index] - b[index]).getDistance()
        return total / a.size
    }

    const val MatchFloor = 0.70f
}

/**
 * Whether the stroke is the backspace swipe from Section 4 rather than a letter.
 *
 * Right to left, mostly horizontal, and mostly one way: a letter that doubles back covers its own
 * width twice, so requiring the net travel to be most of the horizontal span rules those out. The
 * captured alphabet was checked against exactly this test and no letter trips it.
 *
 * Nothing here defends against the system's back gesture. That swipe starts at the screen edge and
 * never reaches the app at all, which is what "not started from the screen edge" amounts to.
 */
fun isBackspaceStroke(points: List<Offset>, minimumSpan: Float): Boolean {
    if (points.size < 2) return false

    val travel = points.last().x - points.first().x
    if (travel >= 0f) return false

    val spanX = points.maxOf { it.x } - points.minOf { it.x }
    val spanY = points.maxOf { it.y } - points.minOf { it.y }

    return spanX >= minimumSpan &&
        abs(travel) > 0.6f * spanX &&
        spanY < 0.4f * spanX
}
