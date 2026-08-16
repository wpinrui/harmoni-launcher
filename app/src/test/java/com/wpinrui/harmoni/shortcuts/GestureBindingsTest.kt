package com.wpinrui.harmoni.shortcuts

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The unbound guard, which is the one branch here that can be asserted honestly: it returns before
 * touching the system, so nothing below it is a shadow reporting on itself.
 *
 * It is what keeps a swipe up bound to nothing from buzzing as though it had launched something.
 */
@RunWith(RobolectricTestRunner::class)
class GestureBindingsTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearBindings() {
        ShortcutGesture.entries.forEach { GestureBindings.clear(context, it) }
        GestureBindings.load(context)
    }

    @Test
    fun `an unbound gesture starts nothing and says so`() {
        ShortcutGesture.entries.forEach { gesture ->
            assertNull(GestureBindings.bindings.value[gesture])
            assertFalse(GestureBindings.start(context, gesture))
        }
    }
}
