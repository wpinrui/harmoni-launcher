package com.wpinrui.harmoni.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wpinrui.harmoni.context.hasMotionPermission
import com.wpinrui.harmoni.context.hasUsageAccess
import com.wpinrui.harmoni.search.canReadWallpaper
import com.wpinrui.harmoni.system.HarmoniAccessibilityService
import com.wpinrui.harmoni.system.HarmoniNotificationListener
import com.wpinrui.harmoni.system.NotificationAccess
import com.wpinrui.harmoni.system.hasNotificationAccess
import com.wpinrui.harmoni.ui.theme.noRipple
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * What each permission buys, and whether it is live right now.
 *
 * Live rather than granted: two of these are only useful once a service has actually bound, which
 * happens some time after the switch is flipped.
 */
@Composable
fun PermissionHealth() {
    val context = LocalContext.current
    val listenerBound by NotificationAccess.connected.collectAsState()
    val shadeBound by HarmoniAccessibilityService.connected.collectAsState()

    // These are granted in Settings, in another task, so nothing here changes while the screen is
    // in front of you. Coming back is the moment the answer has just changed, and the only moment
    // worth asking again.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose {}
    }

    val permissions by produceState(
        initialValue = emptyList<Permission>(),
        context,
        listenerBound,
        shadeBound,
        resumes,
    ) {
        value = listOf(
            Permission("Notification listener", context.hasNotificationAccess(), notificationIntent(context)),
            Permission("Media sessions", listenerBound, notificationIntent(context)),
            Permission("Usage access", context.hasUsageAccess(), usageAccessIntent(context)),
            Permission("Motion", context.hasMotionPermission(), appDetailsIntent(context)),
            Permission("Wallpaper, all files access", context.canReadWallpaper(), allFilesIntent(context)),
            Permission("Notification shade", shadeBound, accessibilityIntent()),
        )
    }

    Panel {
        permissions.forEach { permission ->
            PermissionRow(permission) { context.openSettings(permission.intent) }
        }
    }
}

@Composable
private fun PermissionRow(permission: Permission, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (permission.live) Live else Dead),
        )
        Text(text = permission.label, style = BodyStyle, modifier = Modifier.weight(1f))
        Text(text = if (permission.live) "LIVE" else "OFF", style = ValueStyle)
    }
}

private data class Permission(val label: String, val live: Boolean, val intent: Intent)

/**
 * Straight to the switch, not to the top of Settings.
 *
 * Three of these live behind special-access lists that take several taps to find, and the listener
 * has a page of its own that can be addressed by component.
 */
private fun notificationIntent(context: Context) =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
        ComponentName(context, HarmoniNotificationListener::class.java).flattenToString(),
    )

private fun usageAccessIntent(context: Context) =
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, context.selfUri())

private fun allFilesIntent(context: Context) =
    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, context.selfUri())

/** Motion is an ordinary runtime permission, so its switch is on the app's own page. */
private fun appDetailsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, context.selfUri())

/** No per-app deep link exists for accessibility, so this is the list Harmoni appears in. */
private fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

private fun Context.selfUri() = "package:$packageName".toUri()

/** Some OEMs drop a deep link that the framework honours, so landing adjacent beats bouncing. */
private fun Context.openSettings(intent: Intent) {
    runCatching { startActivity(intent) }
        .recoverCatching { startActivity(appDetailsIntent(this)) }
}
