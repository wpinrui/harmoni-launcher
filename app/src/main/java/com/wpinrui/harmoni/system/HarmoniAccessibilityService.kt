package com.wpinrui.harmoni.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exists to pull down the notification shade, and to do nothing else.
 *
 * An accessibility service is the only supported way for an app to open the shade. This one takes
 * no events and asks for no window content, so it cannot read what is on screen; the whole of its
 * capability is [AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS].
 */
class HarmoniAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        current = this
        _connected.value = true
    }

    override fun onDestroy() {
        if (current === this) {
            current = null
            _connected.value = false
        }
        super.onDestroy()
    }

    // Nothing is observed. The service is a button, not a listener.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {

        @Volatile
        private var current: HarmoniAccessibilityService? = null

        private val _connected = MutableStateFlow(false)

        /** Bound right now, which is later than the grant and is what actually matters. */
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        fun expandShade(): Boolean =
            current?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true
    }
}

/** Whether the service has been enabled by hand in Settings, which is not the same as bound. */
fun Context.hasAccessibilityAccess(): Boolean {
    val enabled = Settings.Secure.getString(contentResolver, ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    return enabled.split(':').any { it.substringBefore('/') == packageName }
}

private const val ENABLED_ACCESSIBILITY_SERVICES = "enabled_accessibility_services"
