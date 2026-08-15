package com.wpinrui.harmoni.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.wpinrui.harmoni.BuildConfig
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.context.hasMotionPermission
import com.wpinrui.harmoni.context.hasUsageAccess
import com.wpinrui.harmoni.diagnostics.Diagnostics
import com.wpinrui.harmoni.graffiti.GraffitiCaptureActivity
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.home.RingSlots
import com.wpinrui.harmoni.home.ringTargets
import com.wpinrui.harmoni.search.canReadWallpaper
import com.wpinrui.harmoni.system.HarmoniNotificationListener
import com.wpinrui.harmoni.system.NotificationAccess
import com.wpinrui.harmoni.system.hasNotificationAccess
import com.wpinrui.harmoni.ui.theme.Karla
import com.wpinrui.harmoni.ui.theme.TurnedV

/**
 * What the launcher is currently doing, from Section 5.
 *
 * Read-only, apart from the three things that are not settings: opening the capture tool, resetting
 * the diagnostic counts, and jumping to the Settings page behind a permission.
 */
@Composable
fun LauncherAppScreen() {
    val context = LocalContext.current
    val entries by context.harmoni.appIndex.entries.collectAsState()

    var open by remember { mutableStateOf(OpenByDefault) }
    val toggle: (String) -> Unit = { title ->
        open = if (title in open) open - title else open + title
    }

    val overrides by RingSlots.overrides.collectAsState()
    val showRingInSearch by RingSlots.showInSearch.collectAsState()
    val slots = remember(overrides, entries) { ringTargets(overrides, entries) }
    var editing by remember { mutableStateOf<Int?>(null) }

    editing?.let { index ->
        RingPicker(
            position = Compass[index],
            current = slots[index].label,
            entries = entries,
            swapped = index in overrides,
            onPick = { entry ->
                RingSlots.bind(context, index, entry.packageName)
                editing = null
            },
            onReset = {
                RingSlots.reset(context, index)
                editing = null
            },
            onDismiss = { editing = null },
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground),
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 64.dp, bottom = 104.dp),
    ) {
        item { Masthead() }

        section("RING BINDINGS", open, toggle) {
            ringBindingRows(
                entries = entries,
                slots = slots,
                showInSearch = showRingInSearch,
                onEdit = { editing = it },
                onToggleSearch = { RingSlots.setShowInSearch(context, it) },
            )
        }

        section("GRAFFITI ALPHABET", open, toggle) {
            item { AlphabetChart() }
            item { Row2("Redraw the alphabet", "OPEN", Accent) { context.openCapture() } }
        }

        section("PERMISSION HEALTH", open, toggle) { permissionRows() }

        section("ATTRIBUTIONS", open, toggle) { item { Attributions() } }

        section("CONTEXTUAL RULES", open, toggle) { contextualRuleRows(entries) }

        section("DIAGNOSTICS", open, toggle) { diagnosticRows() }

        section("BUILD", open, toggle) {
            item { Row2("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") }
            item { Row2("Built", BuildConfig.BUILD_DATE) }
            item { Row2("Commit", BuildConfig.GIT_COMMIT) }
            item { Row2("Build type", BuildConfig.BUILD_TYPE) }
        }

        section("APPS, ${entries.size}", open, toggle) {
            items(entries, key = { it.component.flattenToShortString() + it.user }) { entry ->
                AppRow(entry)
            }
        }
    }
}

/**
 * Which sections are open when the screen appears.
 *
 * The four that answer a question you came here with. The other four are reference: long, rarely
 * the reason for opening the screen, and in the way of the ones above them when unrolled.
 */
private val OpenByDefault = setOf(
    "RING BINDINGS",
    "GRAFFITI ALPHABET",
    "PERMISSION HEALTH",
    "ATTRIBUTIONS",
)

private fun LazyListScope.section(
    title: String,
    open: Set<String>,
    onToggle: (String) -> Unit,
    body: LazyListScope.() -> Unit,
) {
    item { SectionHeader(title = title, open = title in open, onToggle = { onToggle(title) }) }
    if (title in open) body()
}

/**
 * The wordmark, with a turned v where the A would be, as the mockups set it.
 *
 * The glyph is one span of [TurnedV] inside otherwise ordinary Karla, because Karla does not carry
 * U+028C and would otherwise fall back to whatever the device happens to have.
 */
@Composable
private fun Masthead() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 42.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = buildAnnotatedString {
                append("H")
                withStyle(SpanStyle(fontFamily = TurnedV)) { append("ʌ") }
                append("rmoni")
            },
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.ExtraLight,
                fontSize = 34.sp,
                letterSpacing = 0.04.em,
                color = Ink,
            ),
        )
        Text(
            text = BuildConfig.VERSION_NAME,
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                letterSpacing = 0.03.em,
                color = Accent,
            ),
        )
    }
}

@Composable
internal fun SectionHeader(title: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onToggle)
            .padding(top = 34.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(text = if (open) "▾" else "▸", style = TitleStyle.copy(fontSize = 21.sp))
        Text(text = title, style = TitleStyle)
    }
}

/**
 * One key and one value, ruled off from the row above.
 *
 * The key is held to its own width and the value takes the rest, so a package name wraps rather
 * than pushing the thing it belongs to off the screen.
 */
@Composable
internal fun Row2(
    key: String,
    value: String,
    valueColour: Color = Ink,
    onClick: (() -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth()

    Column(modifier = if (onClick == null) base else base.noRipple(onClick)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Rule))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = key,
                style = RowStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.9f, fill = false),
            )
            Text(
                text = value,
                style = RowStyle.copy(color = valueColour, textAlign = TextAlign.End),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Each permission, and the Settings page that grants it.
 *
 * Deep linked per row rather than dropping into Settings at the top: three of these five live
 * behind special-access lists that take several taps to find, and knowing a thing is off is not
 * much use without a way to reach the switch.
 */
private fun LazyListScope.permissionRows() {
    item { NotificationRow() }
    item { MediaSessionRow() }
    item { PermissionRow("Usage access", { it.hasUsageAccess() }, ::usageAccessIntent) }
    item { PermissionRow("Motion", { it.hasMotionPermission() }, ::appDetailsIntent) }
    item { PermissionRow("Wallpaper, all files", { it.canReadWallpaper() }, ::allFilesIntent) }
}

@Composable
private fun NotificationRow() =
    PermissionRow("Notification listener", { it.hasNotificationAccess() }, ::notificationIntent)

/** Bound, not merely granted: reading media sessions needs the service actually running. */
@Composable
private fun MediaSessionRow() {
    val context = LocalContext.current
    val bound by NotificationAccess.connected.collectAsState()

    Row2(
        key = "Media sessions",
        value = if (bound) "LIVE" else "OFF",
        valueColour = if (bound) Live else Dead,
        onClick = { context.open(notificationIntent(context)) },
    )
}

@Composable
private fun PermissionRow(
    label: String,
    granted: (Context) -> Boolean,
    intent: (Context) -> Intent,
) {
    val context = LocalContext.current

    // Recomputed on every composition rather than remembered: these are toggled in Settings, and
    // coming back to this screen is exactly when the answer has just changed.
    val live by produceState(initialValue = false, context, label) { value = granted(context) }

    Row2(
        key = label,
        value = if (live) "LIVE" else "OFF",
        valueColour = if (live) Live else Dead,
        onClick = { context.open(intent(context)) },
    )
}

private fun notificationIntent(context: Context) =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
        ComponentName(context, HarmoniNotificationListener::class.java).flattenToString(),
    )

private fun usageAccessIntent(context: Context) =
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, context.selfUri())

private fun allFilesIntent(context: Context) =
    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, context.selfUri())

/** Motion is an ordinary runtime permission, so its switch lives on the app's own page. */
private fun appDetailsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, context.selfUri())

private fun Context.selfUri() = "package:$packageName".toUri()

/**
 * Falls back to the app's own Settings page.
 *
 * Some of these deep links are honoured by the framework but dropped by an OEM's Settings, and
 * landing somewhere adjacent beats bouncing off a dialog.
 */
private fun Context.open(intent: Intent) {
    runCatching { startActivity(intent) }
        .recoverCatching { startActivity(appDetailsIntent(this)) }
}

private fun Context.openCapture() =
    startActivity(Intent(this, GraffitiCaptureActivity::class.java))

private fun LazyListScope.diagnosticRows() {
    item { DiagnosticsBody() }
}

@Composable
private fun DiagnosticsBody() {
    val context = LocalContext.current
    val counts by Diagnostics.counts.collectAsState()

    Column {
        Row2("Rings dismissed without a pick", counts.ringDismissals.toString())
        Row2("Taps refused near an edge", counts.edgeRejects.toString())

        counts.misreads.entries
            .sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
            .forEach { (letter, count) ->
                Row2("${letter.uppercase()}, erased at once", count.toString())
            }

        Row2("Reset counts", "CLEAR", Accent) { Diagnostics.clear(context) }
    }
}

@Composable
private fun Attributions() {
    val context = LocalContext.current

    Column {
        Text(
            text = "Badge icons from Flaticon, used under the Flaticon free licence. Type is " +
                "Karla and Inter, both under the SIL Open Font License.",
            style = RowStyle.copy(color = Muted),
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Row2("Telegram icons, Magnific", "FLATICON", Accent) {
            context.open(web("https://www.flaticon.com/free-icons/telegram"))
        }
        Row2("WhatsApp icons, Fathema Khanom", "FLATICON", Accent) {
            context.open(web("https://www.flaticon.com/free-icons/whatsapp"))
        }
        Row2("Instagram icons, Grow studio", "FLATICON", Accent) {
            context.open(web("https://www.flaticon.com/free-icons/instagram"))
        }
        Row2("Karla, the Karla Project Authors", "OFL", Accent) {
            context.open(web("https://github.com/googlefonts/karla"))
        }
        Row2("Inter, the Inter Project Authors", "OFL", Accent) {
            context.open(web("https://github.com/rsms/inter"))
        }
    }
}

private fun web(url: String) = Intent(Intent.ACTION_VIEW, url.toUri())

// Section 4 matches names and aliases. None are defined yet, so an app resolves by its name alone
// and there is nothing to put beside the package.
@Composable
private fun AppRow(entry: AppEntry) = Row2(entry.label, entry.packageName, Muted)

@Composable
internal fun Modifier.noRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

internal val Ground = Color(0xFF1A1715)
internal val Ink = Color(0xFFF4EFE9)
internal val Accent = Color(0xFFEAB98F)
internal val Muted = Color(0xFF9C918A)
internal val Rule = Color.White.copy(alpha = 0.16f)
internal val Live = Color(0xFF9FC9A6)
internal val Dead = Color(0xFFCE8B7F)

internal val TitleStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = 0.12.em,
    color = Accent,
)

internal val RowStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    letterSpacing = 0.01.em,
    lineHeight = 1.35.em,
    color = Ink,
)
