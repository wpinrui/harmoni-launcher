package com.wpinrui.harmoni.app

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.wpinrui.harmoni.search.canReadWallpaper
import com.wpinrui.harmoni.system.NotificationAccess
import com.wpinrui.harmoni.system.hasNotificationAccess
import com.wpinrui.harmoni.ui.theme.Karla

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
    val entries by context.harmoni.appIndex.entries.collectAsState()

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

        item { SectionHeader("RING BINDINGS") }
        ringBindingRows(entries)

        item { SectionHeader("GRAFFITI ALPHABET") }
        item { AlphabetChart() }
        item { OpenCaptureRow() }

        item { SectionHeader("CONTEXTUAL RULES") }
        contextualRuleRows(entries)

        item { SectionHeader("PERMISSION HEALTH") }
        item { PermissionHealth() }

        item { SectionHeader("DIAGNOSTICS") }
        item { DiagnosticsPanel() }

        item { SectionHeader("BUILD") }
        item { BuildInfo() }

        item { SectionHeader("APPS, ${entries.size}") }
        items(entries, key = { it.component.flattenToShortString() + it.user }) { entry ->
            AppRow(entry)
        }

        item { SectionHeader("ATTRIBUTIONS") }
        item { Attributions() }
    }
}

@Composable
private fun Masthead() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "HARMONI",
            style = TextStyle(
                fontFamily = Karla,
                fontWeight = FontWeight.ExtraLight,
                fontSize = 44.sp,
                letterSpacing = 0.14.em,
                color = Color.White,
            ),
        )
        Text(text = "A read-only account of what the launcher is doing.", style = BodyStyle)
    }
}

@Composable
private fun PermissionHealth() {
    val context = LocalContext.current
    val listenerBound by NotificationAccess.connected.collectAsState()

    // Read on each composition rather than once: these are toggled in Settings, so the way back to
    // this screen is exactly the moment the answer has just changed.
    val granted by produceState(initialValue = emptyList<Pair<String, Boolean>>(), context, listenerBound) {
        value = listOf(
            "Notification listener" to context.hasNotificationAccess(),
            "Media sessions" to listenerBound,
            "Usage access" to context.hasUsageAccess(),
            "Motion" to context.hasMotionPermission(),
            "Wallpaper, all files access" to context.canReadWallpaper(),
        )
    }

    Panel {
        granted.forEach { (label, live) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (live) Live else Dead),
                )
                Text(text = label, style = BodyStyle, modifier = Modifier.weight(1f))
                Text(text = if (live) "LIVE" else "OFF", style = ValueStyle)
            }
        }

        Text(
            text = "Media sessions ride on the notification listener being bound, which happens " +
                "some time after the grant rather than at the moment of it.",
            style = NoteStyle,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
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
            Text(
                text = "A letter erased straight after it was drawn is the shape to redraw.",
                style = NoteStyle,
                modifier = Modifier.padding(top = 8.dp),
            )
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
private fun OpenCaptureRow() {
    val context = LocalContext.current

    Panel(onClick = { context.startActivity(Intent(context, GraffitiCaptureActivity::class.java)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Redraw the alphabet", style = BodyStyle.copy(color = Color.White))
                Text(
                    text = "Any letter redrawn replaces the bundled samples for that letter alone.",
                    style = NoteStyle,
                )
            }
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
internal fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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

@Composable
private fun AppRow(entry: AppEntry) {
    Panel {
        Text(text = entry.label, style = ValueStyle)
        Text(text = entry.packageName, style = NoteStyle)
        // Section 4 matches against names and aliases. No aliases are defined yet, so every app
        // resolves by its name alone and this says so rather than showing an empty field.
        Text(text = "No aliases", style = NoteStyle)
    }
}
