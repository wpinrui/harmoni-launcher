package com.wpinrui.harmoni.search

import android.content.ComponentName
import android.os.UserHandle
import com.wpinrui.harmoni.apps.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The ranking is a hand-packed integer, boundaries * 100 + runs * 10, whose tiers stay separate
 * only by arithmetic. These pin each tier against the one below it, so a change to either weight
 * that happens to keep both monotone still fails.
 */
@RunWith(RobolectricTestRunner::class)
class AppMatcherTest {

    @Test
    fun `word boundaries outrank adjacent runs`() {
        // Google Maps: two boundaries, no run. Gmail: one boundary, one run.
        val entries = entries("Gmail", "Google Maps")
        assertEquals(listOf("Google Maps", "Gmail"), AppMatcher.match(entries, "gm").labels())
    }

    @Test
    fun `adjacent runs outrank a shorter name`() {
        // Abc scores a run on "ab"; Axb has the same single boundary and no run, and is no longer.
        val entries = entries("Axb", "Abc")
        assertEquals(listOf("Abc", "Axb"), AppMatcher.match(entries, "ab").labels())
    }

    @Test
    fun `a shorter name breaks a score tie`() {
        val entries = entries("Clock Widget Companion", "Clock")
        assertEquals("Clock", AppMatcher.match(entries, "clock").labels().first())
    }

    @Test
    fun `the documented example holds`() {
        val entries = entries("Calculator", "Clock")
        assertEquals(listOf("Clock", "Calculator"), AppMatcher.match(entries, "cl").labels())
    }

    @Test
    fun `matching is a subsequence, not a prefix`() {
        val entries = entries("Telegram")
        assertEquals(listOf("Telegram"), AppMatcher.match(entries, "tgm").labels())
    }

    @Test
    fun `order does not depend on the order of the input`() {
        val labels = listOf("Camera", "Calendar", "Calculator", "Clock", "Chrome")
        val forwards = AppMatcher.match(entries(*labels.toTypedArray()), "ca").labels()
        val backwards = AppMatcher.match(entries(*labels.reversed().toTypedArray()), "ca").labels()

        // Section 4 promises the same query produces the same layout every time.
        assertEquals(forwards, backwards)
    }

    @Test
    fun `entries sharing a label are ordered by component`() {
        val first = entry("Notes", "com.a.notes")
        val second = entry("Notes", "com.b.notes")
        val ordered = AppMatcher.match(listOf(second, first), "notes")

        assertEquals(listOf("com.a.notes", "com.b.notes"), ordered.map { it.packageName })
    }

    @Test
    fun `edit distance only runs when nothing matches in order`() {
        // "clok" is not a subsequence of Clock, so the fallback tier finds it.
        assertEquals(listOf("Clock"), AppMatcher.match(entries("Clock"), "clok").labels())

        // "cl" is a subsequence of both, so neither edit-distance neighbour may be added.
        val both = AppMatcher.match(entries("Clock", "Calculator"), "cl")
        assertEquals(2, both.size)
    }

    @Test
    fun `a typo beyond tolerance finds nothing`() {
        assertTrue(AppMatcher.match(entries("Clock"), "zzzz").isEmpty())
    }

    @Test
    fun `a blank or whitespace query returns everything unchanged`() {
        val entries = entries("Clock", "Camera")
        assertEquals(entries, AppMatcher.match(entries, ""))
        assertEquals(entries, AppMatcher.match(entries, "   "))
    }

    @Test
    fun `matching ignores case in both directions`() {
        assertEquals(listOf("Clock"), AppMatcher.match(entries("Clock"), "CLOCK").labels())
        assertEquals(listOf("YouTube"), AppMatcher.match(entries("YouTube"), "youtube").labels())
    }

    @Test
    fun `an empty index returns nothing rather than failing`() {
        assertTrue(AppMatcher.match(emptyList(), "anything").isEmpty())
    }

    private fun List<AppEntry>.labels() = map { it.label }

    private fun entries(vararg labels: String) =
        labels.mapIndexed { index, label -> entry(label, "com.test.app$index") }

    private fun entry(label: String, packageName: String) = AppEntry(
        component = ComponentName(packageName, "$packageName.Main"),
        user = UserHandle.getUserHandleForUid(0),
        label = label,
    )
}
