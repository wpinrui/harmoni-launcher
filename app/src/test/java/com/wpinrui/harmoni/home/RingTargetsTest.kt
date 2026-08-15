package com.wpinrui.harmoni.home

import android.content.ComponentName
import android.os.UserHandle
import com.wpinrui.harmoni.apps.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The eight are positional, and that is the whole claim: a blank never closes a gap, because doing
 * so would move every app after it and cost the muscle memory the ring exists for.
 */
@RunWith(RobolectricTestRunner::class)
class RingTargetsTest {

    @Test
    fun `with nothing overridden the compiled bindings come back untouched`() {
        val targets = ringTargets(overrides = emptyMap(), entries = emptyList())

        assertEquals(RingBindings.slots.size, targets.size)
        assertEquals(RingBindings.slots, targets)
    }

    @Test
    fun `an override takes its label from the index`() {
        val targets = ringTargets(
            overrides = mapOf(0 to "com.example.notes"),
            entries = listOf(entry("Notes", "com.example.notes")),
        )

        assertEquals("Notes", targets[0]?.label)
    }

    @Test
    fun `an override for something not installed falls back to its package`() {
        val targets = ringTargets(overrides = mapOf(0 to "com.example.gone"), entries = emptyList())

        assertEquals("com.example.gone", targets[0]?.label)
    }

    @Test
    fun `a hidden app leaves its position blank without moving the others`() {
        val before = ringTargets(overrides = emptyMap(), entries = emptyList())
        val hiddenPackage = (before[2] as RingTarget.App).packageName

        val after = ringTargets(emptyMap(), emptyList(), hidden = setOf(hiddenPackage))

        assertEquals(before.size, after.size)
        assertNull(after[2])
        assertEquals(before[3], after[3])
        assertEquals(before[1], after[1])
    }

    @Test
    fun `hiding a browser does not blank the web app that opens in it`() {
        val web = RingBindings.slots.filterIsInstance<RingTarget.Web>().first()
        val slot = RingBindings.slots.indexOf(web)

        val targets = ringTargets(emptyMap(), emptyList(), hidden = setOf(web.browserPackage))

        // The web app borrows the browser's icon but is not the browser.
        assertNotNull(targets[slot])
    }

    private fun entry(label: String, packageName: String) = AppEntry(
        component = ComponentName(packageName, "$packageName.Main"),
        user = UserHandle.getUserHandleForUid(0),
        label = label,
    )
}
