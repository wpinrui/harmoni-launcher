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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wpinrui.harmoni.BuildConfig
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.apps.visible
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
import com.wpinrui.harmoni.ui.theme.TurnedVWeight

/**
 * What the launcher is currently doing, from Section 5.
 *
 * Read-only throughout. Every binding, rule and shape here lives in source or in a captured file,
 * so the screen reports them rather than offering to change them. The two exceptions are the two
 * things that are not settings at all: opening the capture tool, and resetting the diagnostic
 * counts, which are meaningless without a period to count over.
 */
@Composable
fun LauncherAppScreen() {
    val context = LocalContext.current
    val hidden by HiddenApps.packages.collectAsState()
    val all by context.harmoni.appIndex.entries.collectAsState()

    // Everything on this screen except the hidden list itself works from what is visible, so a
    // hidden app cannot be bound to the ring from here either.
    val entries = remember(all, hidden) { all.visible(hidden) }

    var open by remember { mutableStateOf(OpenByDefault) }
    val toggle: (String) -> Unit = { title ->
        open = if (title in open) open - title else open + title
    }

    val overrides by RingSlots.overrides.collectAsState()
    val showRingInSearch by RingSlots.showInSearch.collectAsState()
    val slots = remember(overrides, entries, hidden) { ringTargets(overrides, entries, hidden) }
    var editing by remember { mutableStateOf<Int?>(null) }

    editing?.let { index ->
        RingPicker(
            entries = entries,
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
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 22.dp,
            top = 64.dp,
            bottom = 72.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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

        section("HIDDEN APPS", open, toggle) {
            item { HiddenAppsRow(count = hidden.size) }
        }

        section("GRAFFITI ALPHABET", open, toggle) {
            item { AlphabetChart() }
            item { OpenCaptureRow() }
        }

        section("PERMISSION HEALTH", open, toggle) { item { PermissionHealth() } }

        section("ATTRIBUTIONS", open, toggle) { item { Attributions() } }

        section("CONTEXTUAL RULES", open, toggle) { contextualRuleRows(entries) }

        section("DIAGNOSTICS", open, toggle) { item { DiagnosticsPanel() } }

        section("BUILD", open, toggle) { item { BuildInfo() } }
    }
}

/** The four that answer a question you came here with. The other four are reference. */
private val OpenByDefault = setOf(
    "RING BINDINGS",
    "GRAFFITI ALPHABET",
    "PERMISSION HEALTH",
    "ATTRIBUTIONS",
)

/**
 * A header and, when it is open, everything under it.
 *
 * Closed by default. Eight headers on one screen is an index of what the launcher is doing; the
 * same page unrolled is several thousand pixels of reference material to scroll past to reach the
 * one thing being looked up.
 */
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
 * Karla has no U+028C: the shipped file and the upstream variable font both carry 393 codepoints
 * and it is in neither, which is why the mockups' `Karla, system-ui, sans-serif` renders it in a
 * browser fallback. Inter, subset to that one glyph, is that fallback made deliberate.
 */
@Composable
private fun Masthead() {
    Text(
        text = buildAnnotatedString {
            append("H")
            // A lowercase letter sitting at x-height, scaled to stand level with Karla's capitals.
            // Measured across both fonts rather than within one: Inter's turned v is 0.546 em tall
            // and Karla's H is 0.628 em, and Karla's caps are short for its em.
            withStyle(
                SpanStyle(
                    fontFamily = TurnedV,
                    fontWeight = TurnedVWeight,
                    fontSize = MastheadSize * CapScale,
                ),
            ) {
                append("ʌ")
            }
            append("RMONI")
        },
        style = MastheadStyle,
    )
}

private val MastheadSize = 44.sp
private const val CapScale = 1.150f

private val MastheadStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.ExtraLight,
    fontSize = MastheadSize,
    lineHeight = MastheadSize * 1.1f,
    letterSpacing = 0.14.em,
    color = Color.White,
)

@Composable
private fun PermissionHealth() {
    val context = LocalContext.current
    val listenerBound by NotificationAccess.connected.collectAsState()

    // These are granted in Settings, in another task, so nothing here changes while the screen is
    // in front of you. Coming back is the moment the answer has just changed, and the only moment
    // worth asking again.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose {}
    }

    val granted by produceState(initialValue = emptyList<Permission>(), context, listenerBound, resumes) {
        value = listOf(
            Permission("Notification listener", context.hasNotificationAccess(), notificationIntent(context)),
            Permission("Media sessions", listenerBound, notificationIntent(context)),
            Permission("Usage access", context.hasUsageAccess(), usageAccessIntent(context)),
            Permission("Motion", context.hasMotionPermission(), appDetailsIntent(context)),
            Permission("Wallpaper, all files access", context.canReadWallpaper(), allFilesIntent(context)),
        )
    }

    Panel {
        granted.forEach { permission ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRipple { context.openSettings(permission.intent) }
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
    }
}

private data class Permission(val label: String, val live: Boolean, val intent: Intent)

/**
 * Straight to the switch, not to the top of Settings.
 *
 * Three of these five live behind special-access lists that take several taps to find, and the
 * listener has a page of its own that can be addressed by component.
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

private fun Context.selfUri() = "package:$packageName".toUri()

/** Some OEMs drop a deep link that the framework honours, so landing adjacent beats bouncing. */
private fun Context.openSettings(intent: Intent) {
    runCatching { startActivity(intent) }
        .recoverCatching { startActivity(appDetailsIntent(this)) }
}

@Composable
private fun DiagnosticsPanel() {
    val context = LocalContext.current
    val counts by Diagnostics.counts.collectAsState()

    Panel {
        if (counts.total == 0) {
            Text(text = "Nothing recorded yet.", style = BodyStyle)
            return@Panel
        }

        KeyValue("Rings dismissed without a pick", counts.ringDismissals.toString())
        KeyValue("Taps refused near an edge", counts.edgeRejects.toString())

        if (counts.misreads.isNotEmpty()) {
            Text(
                text = "LETTERS ERASED IMMEDIATELY",
                style = NoteStyle,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            counts.misreads.entries
                .sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
                .forEach { (letter, count) ->
                    KeyValue(letter.uppercase(), count.toString())
                }
        }

        Text(
            text = "RESET COUNTS",
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .noRipple { Diagnostics.clear(context) }
                .padding(vertical = 6.dp),
            style = ValueStyle.copy(color = Accent),
        )
    }
}

@Composable
private fun BuildInfo() {
    Panel {
        KeyValue("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        KeyValue("Built", BuildConfig.BUILD_DATE)
        KeyValue("Commit", BuildConfig.GIT_COMMIT)
        KeyValue("Build type", BuildConfig.BUILD_TYPE)
    }
}

@Composable
private fun HiddenAppsRow(count: Int) {
    val context = LocalContext.current

    Panel(onClick = { context.startActivity(Intent(context, HiddenAppsActivity::class.java)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (count == 1) "1 app hidden" else "$count apps hidden",
                style = ValueStyle,
                modifier = Modifier.weight(1f),
            )
            Text(text = "OPEN", style = ValueStyle.copy(color = Accent))
        }
    }
}

@Composable
private fun OpenCaptureRow() {
    val context = LocalContext.current

    Panel(onClick = { context.startActivity(Intent(context, GraffitiCaptureActivity::class.java)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Redraw the alphabet", style = ValueStyle, modifier = Modifier.weight(1f))
            Text(text = "OPEN", style = ValueStyle.copy(color = Accent))
        }
    }
}

@Composable
private fun Attributions() {
    val context = LocalContext.current

    Panel {
        Text(
            text = "Badge icons from Flaticon, used under the Flaticon free licence.",
            style = BodyStyle,
        )
        Credit(context, "Telegram icons by Magnific", "https://www.flaticon.com/free-icons/telegram")
        Credit(context, "WhatsApp icons by Fathema Khanom", "https://www.flaticon.com/free-icons/whatsapp")
        Credit(context, "Instagram icons by Grow studio", "https://www.flaticon.com/free-icons/instagram")

        Text(
            text = "Type is Karla by the Karla Project Authors, under the SIL Open Font License. " +
                "The full text ships with the source at licenses/Karla-OFL.txt.",
            style = BodyStyle,
            modifier = Modifier.padding(top = 10.dp),
        )
        Credit(context, "github.com/googlefonts/karla", "https://github.com/googlefonts/karla")
    }
}

@Composable
private fun Credit(context: Context, label: String, url: String) {
    Text(
        text = label,
        modifier = Modifier
            .padding(top = 4.dp)
            .noRipple { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
        style = BodyStyle.copy(color = Accent),
    )
}

// Shared furniture, so every section reads the same way down the page.

@Composable
internal fun SectionHeader(title: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRipple(onToggle)
            .padding(top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (open) "▾" else "▸",
            style = HeaderStyle.copy(color = Accent, fontSize = 14.sp),
        )
        Text(text = title, style = HeaderStyle)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.14f)),
        )
    }
}

@Composable
internal fun Panel(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val surface = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(PanelColour)

    Column(
        modifier = (if (onClick == null) surface else surface.noRipple(onClick))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
internal fun KeyValue(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = key, style = BodyStyle, modifier = Modifier.weight(1f))
        Text(text = value, style = ValueStyle)
    }
}

@Composable
internal fun Modifier.noRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

internal val Background = Color(0xFF120E0C)
internal val PanelColour = Color(0xFF1C1714)
internal val Accent = Color(0xFFE8B979)
private val Live = Color(0xFF7FC98B)
private val Dead = Color(0xFF8C6A63)

internal val HeaderStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.22.em,
    color = Color(0xFFCFC6BD),
)

internal val BodyStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Light,
    fontSize = 14.sp,
    lineHeight = 1.4.em,
    color = Color(0xFFB9AFA7),
)

internal val ValueStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.04.em,
    color = Color.White,
)

internal val NoteStyle = TextStyle(
    fontFamily = Karla,
    fontWeight = FontWeight.Light,
    fontSize = 11.5.sp,
    lineHeight = 1.4.em,
    color = Color(0xFF8B817A),
)
