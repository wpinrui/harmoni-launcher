package com.wpinrui.harmoni.apps

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Every launchable activity on the device, kept current by [LauncherApps] callbacks.
 *
 * [LauncherApps] rather than package broadcasts because it is the API built for launchers: it
 * reports availability changes as well as install and removal, and it enumerates work profiles,
 * which broadcasts do not.
 *
 * Callbacks arrive on the main thread and each one triggers a full rescan. A rescan is one binder
 * round trip per profile, which is cheap next to how rarely the installed set changes.
 */
class AppIndex(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _entries = MutableStateFlow<List<AppEntry>>(emptyList())

    /** Sorted by [LABEL_ORDER]. Empty until [start] has run. */
    val entries: StateFlow<List<AppEntry>> = _entries.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = rescan()

        override fun onPackageRemoved(packageName: String, user: UserHandle) = rescan()

        override fun onPackageChanged(packageName: String, user: UserHandle) = rescan()

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = rescan()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = rescan()
    }

    /** The main entry for a package, on the current profile ordering. */
    fun firstFor(packageName: String): AppEntry? =
        entries.value.firstOrNull { it.packageName == packageName }

    fun start() {
        launcherApps.registerCallback(callback)
        rescan()
    }

    private fun rescan() {
        _entries.value = userManager.userProfiles
            .flatMap { user -> activitiesFor(user).map { it.toEntry(user) } }
            .sortedWith(LABEL_ORDER)
    }

    /**
     * Cross-profile queries are refused until Harmoni is the active home app, which is a normal
     * state to be in and not a failure: the profile is skipped and the rest of the index is
     * still built. The next rescan after being made default picks it up.
     */
    private fun activitiesFor(user: UserHandle): List<LauncherActivityInfo> = try {
        launcherApps.getActivityList(null, user)
    } catch (e: SecurityException) {
        Log.w(TAG, "Cannot read profile $user; Harmoni is probably not the home app yet", e)
        emptyList()
    }

    private fun LauncherActivityInfo.toEntry(user: UserHandle) = AppEntry(
        component = componentName,
        user = user,
        label = label.toString(),
    )

    companion object {
        private const val TAG = "AppIndex"

        /**
         * Case-insensitive by label, then by component so entries sharing a label keep a fixed
         * order. Section 4 requires the same query to produce the same layout every time, which
         * only holds if the index it filters is itself deterministic.
         */
        val LABEL_ORDER: Comparator<AppEntry> =
            compareBy<AppEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                .thenBy { it.component.flattenToShortString() }
    }
}
