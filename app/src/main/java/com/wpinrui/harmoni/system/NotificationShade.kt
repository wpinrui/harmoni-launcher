package com.wpinrui.harmoni.system

import android.content.Context
import android.util.Log

/**
 * Pulls down the notification shade.
 *
 * Two routes, tried in that order.
 *
 * The accessibility service first, because it is the only supported way for an app to open the
 * shade and it always works once bound.
 *
 * `StatusBarManager` second, for before the grant. It has the method, guarded by EXPAND_STATUS_BAR,
 * which is an ordinary permission, but the method itself is hidden and subject to the restrictions
 * on non-SDK interfaces, so whether the call lands is a property of the device rather than of the
 * permission. Failure there is logged and nothing else.
 */
object NotificationShade {

    private const val TAG = "NotificationShade"
    private const val CLASS = "android.app.StatusBarManager"
    private const val METHOD = "expandNotificationsPanel"

    fun expand(context: Context): Boolean {
        // The accessibility service first, since it is the supported route and always works when
        // it is bound. The hidden method is the fallback for when it is not.
        if (HarmoniAccessibilityService.expandShade()) return true

        val result = runCatching {
            val service = context.getSystemService("statusbar")
                ?: error("no statusbar service")
            Class.forName(CLASS).getMethod(METHOD).invoke(service)
        }

        result.exceptionOrNull()?.let { Log.w(TAG, "$CLASS.$METHOD is not reachable here", it) }
        return result.isSuccess
    }
}
