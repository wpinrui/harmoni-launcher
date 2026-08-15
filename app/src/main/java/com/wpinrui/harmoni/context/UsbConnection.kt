package com.wpinrui.harmoni.context

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Whether a USB data connection is up, which is the one signal that outranks everything else:
 * plugging into a computer almost always means heading for Settings.
 *
 * Charging does not count. The broadcast reports both, and only a configured connection means
 * data functions were negotiated with a host.
 */
class UsbConnection(private val context: Context) {

    private var receiver: BroadcastReceiver? = null

    var dataConnected: Boolean = read()
        private set

    fun start() {
        if (receiver != null) return
        val listener = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                dataConnected = intent.isDataConnection()
            }
        }
        context.registerReceiver(listener, IntentFilter(ACTION_USB_STATE))
        receiver = listener
        dataConnected = read()
    }

    private fun read(): Boolean =
        context.registerReceiver(null, IntentFilter(ACTION_USB_STATE))?.isDataConnection() == true

    private fun Intent.isDataConnection(): Boolean =
        getBooleanExtra(EXTRA_CONNECTED, false) && getBooleanExtra(EXTRA_CONFIGURED, false)

    private companion object {
        // UsbManager holds these as hidden constants, so they are spelled out here.
        const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
        const val EXTRA_CONNECTED = "connected"
        const val EXTRA_CONFIGURED = "configured"
    }
}
