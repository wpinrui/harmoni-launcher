package com.wpinrui.harmoni.apps

import android.content.ComponentName
import android.os.UserHandle

/**
 * One launchable activity, as the launcher sees it.
 *
 * [user] is part of the identity, not decoration: a work profile carries its own copy of an app
 * under the same component name, and the two are different entries.
 */
data class AppEntry(
    val component: ComponentName,
    val user: UserHandle,
    val label: String,
) {
    val packageName: String get() = component.packageName
}
