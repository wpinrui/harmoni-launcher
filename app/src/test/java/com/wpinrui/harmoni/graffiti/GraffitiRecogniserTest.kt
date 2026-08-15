package com.wpinrui.harmoni.graffiti

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * The recogniser departs from the published `$1` algorithm in two documented ways, and both are
 * pinned here: scaling is uniform, and rotation is deliberately not normalised. A well-meaning
 * change putting the indicative angle back would pass every other test in this file.
 */
class GraffitiRecogniserTest {

    @Test
    fun `a stroke with no length cannot be prepared`() {
        assertNull(GraffitiRecogniser.prepare(emptyList()))
        assertNull(GraffitiRecogniser.prepare(listOf(Offset(5f, 5f))))
        assertNull(GraffitiRecogniser.prepare(List(20) { Offset(5f, 5f) }))
    }

    @Test
    fun `preparing always produces the same number of points`() {
        val two = GraffitiRecogniser.prepare(listOf(Offset(0f, 0f), Offset(100f, 100f)))
        val many = GraffitiRecogniser.prepare(List(1000) { Offset(it.toFloat(), it.toFloat()) })

        assertEquals(64, two?.size)
        assertEquals(64, many?.size)
    }

    @Test
    fun `matching survives being drawn ten times the size`() {
        val small = line(from = Offset(0f, 0f), to = Offset(10f, 40f))
        val large = line(from = Offset(0f, 0f), to = Offset(100f, 400f))

        val match = GraffitiRecogniser.recognise(large, templates('a' to small))

        assertEquals('a', match?.letter)
    }

    @Test
    fun `scaling is uniform, so aspect ratio still separates two shapes`() {
        // An L drawn tall against an L drawn wide. Scaling each axis to a square, which is what
        // the published algorithm does, would collapse these onto each other.
        val tall = polyline(listOf(Offset(0f, 0f), Offset(0f, 400f), Offset(100f, 400f)))
        val wide = polyline(listOf(Offset(0f, 0f), Offset(0f, 100f), Offset(400f, 100f)))

        val match = GraffitiRecogniser.recognise(wide, templates('a' to tall), floor = 0f)

        assertNotNull(match)
        assertTrue("aspect should separate them, scored ${match?.score}", match!!.score < 0.7f)
    }

    @Test
    fun `matching survives being drawn elsewhere on the surface`() {
        val here = arc(centre = Offset(50f, 50f))
        val there = arc(centre = Offset(900f, 1600f))

        assertEquals('a', GraffitiRecogniser.recognise(there, templates('a' to here))?.letter)
    }

    @Test
    fun `rotation is not normalised away`() {
        // What separates M from W and N from Z. Rotation invariance would match these.
        val upright = polyline(listOf(Offset(0f, 100f), Offset(50f, 0f), Offset(100f, 100f)))
        val turned = polyline(listOf(Offset(0f, 0f), Offset(50f, 100f), Offset(100f, 0f)))

        val match = GraffitiRecogniser.recognise(turned, templates('a' to upright), floor = 0f)

        assertNotNull(match)
        assertTrue("a turned stroke should score poorly, was ${match?.score}", match!!.score < 0.7f)
    }

    @Test
    fun `the nearest template wins, not the average of them`() {
        // 'a' has one tight sample and one sloppy one. 'b' sits between their mean and the input.
        val tight = line(Offset(0f, 0f), Offset(0f, 100f))
        val sloppy = line(Offset(0f, 0f), Offset(60f, 100f))
        val between = line(Offset(0f, 0f), Offset(30f, 100f))

        val match = GraffitiRecogniser.recognise(
            points = tight,
            templates = templates('a' to tight, 'a' to sloppy, 'b' to between),
        )

        assertEquals('a', match?.letter)
    }

    @Test
    fun `an exact copy of a template scores one`() {
        val stroke = arc(centre = Offset(0f, 0f))
        val match = GraffitiRecogniser.recognise(stroke, templates('a' to stroke))

        assertEquals(1.0f, match!!.score, 0.001f)
    }

    @Test
    fun `the floor rejects, and lowering it lets the same stroke through`() {
        val template = line(Offset(0f, 0f), Offset(0f, 100f))
        val unlike = arc(centre = Offset(0f, 0f))

        assertNull(GraffitiRecogniser.recognise(unlike, templates('a' to template)))
        assertNotNull(GraffitiRecogniser.recognise(unlike, templates('a' to template), floor = 0f))
    }

    @Test
    fun `an empty alphabet matches nothing`() {
        assertNull(GraffitiRecogniser.recognise(line(Offset(0f, 0f), Offset(0f, 100f)), emptyList()))
    }

    private fun templates(vararg samples: Pair<Char, List<Offset>>) =
        samples.mapNotNull { (letter, points) ->
            GraffitiRecogniser.prepare(points)?.let { GraffitiTemplate(letter, it) }
        }

    private fun line(from: Offset, to: Offset, steps: Int = 40) = List(steps) { step ->
        val t = step / (steps - 1f)
        Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
    }

    private fun polyline(corners: List<Offset>) =
        corners.zipWithNext().flatMap { (from, to) -> line(from, to, steps = 20) }

    private fun arc(centre: Offset, radius: Float = 50f, steps: Int = 40) = List(steps) { step ->
        val angle = Math.PI * step / (steps - 1)
        Offset(centre.x + (cos(angle) * radius).toFloat(), centre.y + (sin(angle) * radius).toFloat())
    }
}
