package com.wpinrui.harmoni.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.wpinrui.harmoni.BuildConfig
import com.wpinrui.harmoni.apps.HiddenApps
import com.wpinrui.harmoni.apps.visible
import com.wpinrui.harmoni.diagnostics.Diagnostics
import com.wpinrui.harmoni.graffiti.GraffitiCaptureActivity
import com.wpinrui.harmoni.harmoni
import com.wpinrui.harmoni.home.RingSlots
import com.wpinrui.harmoni.home.ringTargets
import com.wpinrui.harmoni.shortcuts.GestureBindings
import com.wpinrui.harmoni.shortcuts.ShortcutGesture
import com.wpinrui.harmoni.ui.theme.Accent
import com.wpinrui.harmoni.ui.theme.Ground
import com.wpinrui.harmoni.ui.theme.Karla
import com.wpinrui.harmoni.ui.theme.TurnedV
import com.wpinrui.harmoni.ui.theme.TurnedVWeight
import com.wpinrui.harmoni.ui.theme.noRipple

/**
 * What the launcher is doing, per GDD Section 6.
 *
 * Four sections are editable: the ring's eight, what each swipe up runs, which apps are hidden,
 * and whether the ring's apps also appear in search. The rest reports, and the contextual rules,
 * diagnostics and build info are read-only by construction, since they live in source or are
 * counted rather than chosen.
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
    val gestureBindings by GestureBindings.bindings.collectAsState()
    val slots = remember(overrides, entries, hidden) { ringTargets(overrides, entries, hidden) }

    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var bindingGesture by remember { mutableStateOf<ShortcutGesture?>(null) }

    LauncherDialogs(
        entries = entries,
        editingSlot = editingSlot,
        bindingGesture = bindingGesture,
        bound = gestureBindings.keys,
        onSlotSettled = { editingSlot = null },
        onGestureSettled = { bindingGesture = null },
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Masthead() }

        section("RING BINDINGS", open, toggle) {
            ringBindingRows(
                entries = entries,
                slots = slots,
                showInSearch = showRingInSearch,
                onEdit = { editingSlot = it },
                onToggleSearch = { RingSlots.setShowInSearch(context, it) },
            )
        }

        section("GESTURE SHORTCUTS", open, toggle) {
            gestureShortcutRows(bindings = gestureBindings, onBind = { bindingGesture = it })
        }

        section("HIDDEN APPS", open, toggle) {
            item {
                ActionPanel(
                    label = if (hidden.size == 1) "1 app hidden" else "${hidden.size} apps hidden",
                    action = "OPEN",
                    onClick = { context.open<HiddenAppsActivity>() },
                )
            }
        }

        section("GRAFFITI ALPHABET", open, toggle) {
            item { AlphabetChart() }
            item {
                ActionPanel("Redraw the alphabet", "OPEN") { context.open<GraffitiCaptureActivity>() }
            }
        }

        section("PERMISSION HEALTH", open, toggle) { item { PermissionHealth() } }

        section("ATTRIBUTIONS", open, toggle) { item { Attributions() } }

        section("CONTEXTUAL RULES", open, toggle) { contextualRuleRows(entries) }

        section("DIAGNOSTICS", open, toggle) { item { DiagnosticsPanel() } }

        section("BUILD", open, toggle) { item { BuildInfo() } }
    }
}

/** The four that answer a question you came here with. The other five are reference. */
private val OpenByDefault = setOf(
    "RING BINDINGS",
    "GRAFFITI ALPHABET",
    "PERMISSION HEALTH",
    "ATTRIBUTIONS",
)

/**
 * A header and, when it is open, everything under it.
 *
 * Nine headers on one screen is an index of what the launcher is doing; the same page unrolled is
 * several thousand pixels of reference material to scroll past to reach the one thing being
 * looked up.
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

/** Both pickers, kept together so the screen itself is a list of sections and nothing else. */
@Composable
private fun LauncherDialogs(
    entries: List<com.wpinrui.harmoni.apps.AppEntry>,
    editingSlot: Int?,
    bindingGesture: ShortcutGesture?,
    bound: Set<ShortcutGesture>,
    onSlotSettled: () -> Unit,
    onGestureSettled: () -> Unit,
) {
    val context = LocalContext.current

    bindingGesture?.let { gesture ->
        ShortcutPicker(
            gestureLabel = gesture.label,
            bound = gesture in bound,
            onPick = {
                GestureBindings.bind(context, gesture, it)
                onGestureSettled()
            },
            onClear = {
                GestureBindings.clear(context, gesture)
                onGestureSettled()
            },
            onDismiss = onGestureSettled,
        )
    }

    editingSlot?.let { index ->
        RingPicker(
            entries = entries,
            onPick = {
                RingSlots.bind(context, index, it.packageName)
                onSlotSettled()
            },
            onReset = {
                RingSlots.reset(context, index)
                onSlotSettled()
            },
            onDismiss = onSlotSettled,
        )
    }
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
private fun DiagnosticsPanel() {
    val context = LocalContext.current
    val counts by Diagnostics.counts.collectAsState()

    Panel {
        if (counts.total == 0) {
            Text(text = "Nothing recorded yet.", style = BodyStyle)
            return@Panel
        }

        KeyValue("Rings dismissed without a pick", counts.ringDismissals.toString())

        if (counts.misreads.isNotEmpty()) {
            Text(
                text = "LETTERS ERASED IMMEDIATELY",
                style = NoteStyle,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            counts.misreads.entries
                .sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
                .forEach { (letter, count) -> KeyValue(letter.uppercase(), count.toString()) }
        }

        Text(
            text = "RESET COUNTS",
            modifier = Modifier
                .padding(top = 12.dp)
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

/**
 * Required by the licences of the assets shipped in the APK, so this section is not optional.
 * `README.md` carries the same credits and the two have to stay in step.
 */
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
            text = "Type is Karla by the Karla Project Authors and Inter by the Inter Project " +
                "Authors, both under the SIL Open Font License. The full texts ship with the " +
                "source at licenses/Karla-OFL.txt and licenses/Inter-OFL.txt.",
            style = BodyStyle,
            modifier = Modifier.padding(top = 10.dp),
        )
        Credit(context, "github.com/googlefonts/karla", "https://github.com/googlefonts/karla")
        Credit(context, "github.com/rsms/inter", "https://github.com/rsms/inter")

        Text(
            text = "Stroke matching follows the $1 unistroke recogniser, Wobbrock, Wilson and Li.",
            style = BodyStyle,
            modifier = Modifier.padding(top = 10.dp),
        )
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

private inline fun <reified T> Context.open() = startActivity(Intent(this, T::class.java))
