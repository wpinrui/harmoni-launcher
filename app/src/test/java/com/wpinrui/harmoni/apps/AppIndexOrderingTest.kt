package com.wpinrui.harmoni.apps

import android.content.ComponentName
import android.os.Process
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppIndexOrderingTest {

    @Test
    fun `orders by label, ignoring case`() {
        val sorted = listOf(entry("Telegram"), entry("instagram"), entry("YouTube"))
            .sortedWith(AppIndex.LABEL_ORDER)

        assertEquals(listOf("instagram", "Telegram", "YouTube"), sorted.map { it.label })
    }

    @Test
    fun `breaks label ties by component, so equal labels keep a fixed order`() {
        val second = entry("Clock", packageName = "com.b.clock")
        val first = entry("Clock", packageName = "com.a.clock")

        assertEquals(
            listOf(first, second),
            listOf(second, first).sortedWith(AppIndex.LABEL_ORDER),
        )
    }

    private fun entry(
        label: String,
        packageName: String = "com.example.${label.lowercase()}",
    ) = AppEntry(
        component = ComponentName(packageName, "$packageName.MainActivity"),
        user = Process.myUserHandle(),
        label = label,
    )
}
