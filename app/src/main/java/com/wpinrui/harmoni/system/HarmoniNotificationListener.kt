package com.wpinrui.harmoni.system

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Counts the notifications the badged apps currently have posted.
 *
 * The service is bound by the system, not by Harmoni, so the counts live in [NotificationCounts]
 * rather than on an instance: the home surface reads them whether or not the service happens to
 * be alive at that moment.
 *
 * Nothing arrives until notification access is granted by hand in Settings. Until then the counts
 * stay empty and the badges simply show no number.
 */
class HarmoniNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationAccess.setConnected(true)
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationAccess.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        // activeNotifications throws if the binding has already gone away, which happens on the
        // way down; an empty map is the honest answer at that point.
        val active = runCatching { activeNotifications }.getOrNull() ?: return

        // A group summary stands for the conversations under it rather than being one of them,
        // so counting it shows one more than the app has.
        val posted = active.filterNot {
            it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        }
        val (ongoing, unread) = posted.partition { it.isOngoing }

        NotificationCounts.set(
            counts = unread.groupingBy { it.packageName }.eachCount(),
            sticky = ongoing.map { it.packageName }.toSet(),
        )
    }
}

/**
 * What is currently posted, published by [HarmoniNotificationListener].
 *
 * The two are kept apart because they mean different things. A count is unread mail or messages,
 * which is what the badges show. An ongoing notification is something in progress, a ride or a
 * download, which the badges ignore and the contextual ring cares about a great deal.
 */
object NotificationCounts {

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private val _sticky = MutableStateFlow<Set<String>>(emptySet())
    val sticky: StateFlow<Set<String>> = _sticky.asStateFlow()

    internal fun set(counts: Map<String, Int>, sticky: Set<String>) {
        _counts.value = counts
        _sticky.value = sticky
    }
}

/**
 * Whether the listener is bound right now, which is later than the grant itself.
 *
 * Reading media sessions needs the service actually bound, not merely enabled in Settings. The
 * bind lands some time after the process starts, so anything depending on it has to wait for this
 * rather than try once at startup and give up.
 */
object NotificationAccess {

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    internal fun setConnected(connected: Boolean) {
        _connected.value = connected
    }
}

/** Whether the user has granted Harmoni notification access. */
fun Context.hasNotificationAccess(): Boolean {
    val enabled = Settings.Secure.getString(contentResolver, ENABLED_LISTENERS).orEmpty()
    val us = ComponentName(this, HarmoniNotificationListener::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == us }
}

private const val ENABLED_LISTENERS = "enabled_notification_listeners"
