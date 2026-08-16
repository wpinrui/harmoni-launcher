package com.wpinrui.harmoni.shortcuts

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.Log

/**
 * One of the shortcuts an app publishes, the ones other launchers show on a long press.
 *
 * [id] is the app's own name for it and is what survives a restart; the label and icon are read
 * fresh each time, since an app is free to rewrite either between publishes.
 */
data class AppShortcut(
    val packageName: String,
    val id: String,
    val label: String,
    val appLabel: String,
    val user: UserHandle,
)

/**
 * Reads and starts app shortcuts.
 *
 * Only the active home app may see these: the system checks [LauncherApps.hasShortcutHostPermission]
 * on every query and throws otherwise. Harmoni is the home app, so this works, but it fails while
 * something else is, which is a normal state to be in and not an error worth crashing over.
 */
object AppShortcuts {

    private const val TAG = "AppShortcuts"

    private val flags = LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED

    /** Every shortcut on the device, in one query, ordered by app then by rank. */
    fun all(context: Context): List<AppShortcut> {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (!launcherApps.hasShortcutHostPermission()) {
            Log.w(TAG, "Not the home app, so shortcuts are not readable")
            return emptyList()
        }

        val query = LauncherApps.ShortcutQuery().setQueryFlags(flags)
        val user = Process.myUserHandle()

        val shortcuts = runCatching { launcherApps.getShortcuts(query, user) }
            .onFailure { Log.w(TAG, "Cannot read shortcuts", it) }
            .getOrNull()
            .orEmpty()

        val labels = mutableMapOf<String, String>()

        return shortcuts
            .mapNotNull { it.toAppShortcut(context, labels) }
            .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }, { it.id }))
    }

    fun iconOf(context: Context, shortcut: AppShortcut): Drawable? {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(flags)
            .setPackage(shortcut.packageName)
            .setShortcutIds(listOf(shortcut.id))

        val info = runCatching { launcherApps.getShortcuts(query, shortcut.user) }
            .getOrNull()
            ?.firstOrNull()
            ?: return null

        return runCatching {
            launcherApps.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi)
        }.getOrNull()
    }

    /**
     * True when the shortcut actually started.
     *
     * False, and nothing louder than a log, if it has since been withdrawn: an app is free to
     * stop publishing one, and a binding outlives it.
     */
    fun start(
        context: Context,
        packageName: String,
        id: String,
        user: UserHandle = Process.myUserHandle(),
    ): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java)

        return runCatching { launcherApps.startShortcut(packageName, id, null, null, user) }
            .onFailure { Log.w(TAG, "Cannot start $packageName/$id", it) }
            .isSuccess
    }

    private fun ShortcutInfo.toAppShortcut(
        context: Context,
        labels: MutableMap<String, String>,
    ): AppShortcut? {
        val label = (longLabel ?: shortLabel)?.toString()?.takeIf { it.isNotBlank() } ?: return null

        val appLabel = labels.getOrPut(`package`) {
            runCatching {
                val info = context.packageManager.getApplicationInfo(`package`, 0)
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(`package`)
        }

        return AppShortcut(
            packageName = `package`,
            id = id,
            label = label,
            appLabel = appLabel,
            user = userHandle,
        )
    }
}
