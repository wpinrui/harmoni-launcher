package com.wpinrui.harmoni.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.apps.AppIcon
import com.wpinrui.harmoni.context.ContextApps
import com.wpinrui.harmoni.context.ContextualRules
import com.wpinrui.harmoni.context.MotionState
import com.wpinrui.harmoni.graffiti.GraffitiLetters
import com.wpinrui.harmoni.graffiti.GraffitiSample
import com.wpinrui.harmoni.graffiti.GraffitiStore
import com.wpinrui.harmoni.graffiti.GraffitiStrokeArt
import com.wpinrui.harmoni.home.RingTarget
import com.wpinrui.harmoni.home.iconPackage
import com.wpinrui.harmoni.shortcuts.BoundShortcut
import com.wpinrui.harmoni.shortcuts.ShortcutGesture
import com.wpinrui.harmoni.ui.theme.Accent
import java.time.Duration
import androidx.compose.runtime.getValue

/**
 * The eight fixed positions, in the order the ring lays them out.
 *
 * Position is the binding, so it is named rather than implied: the ring is muscle memory, and the
 * reason to read this screen is usually to remember which way is which.
 */
internal fun LazyListScope.ringBindingRows(
    entries: List<AppEntry>,
    slots: List<RingTarget?>,
    showInSearch: Boolean,
    onEdit: (Int) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
) {
    itemsIndexed(slots) { index, target ->
        val installed = target != null && entries.any { it.packageName == target.iconPackage }

        Panel(onClick = { onEdit(index) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = Compass[index],
                    style = ValueStyle.copy(color = Accent, textAlign = TextAlign.Center),
                    modifier = Modifier.width(30.dp),
                )

                // A position whose app is hidden is drawn blank rather than filled by the next
                // one along, because the eight are positional.
                if (target == null) {
                    Box(modifier = Modifier.size(30.dp))
                    Text(text = "Blank", style = ValueStyle.copy(color = NoteStyle.color), modifier = Modifier.weight(1f))
                } else {
                    RingTargetIcon(target)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = target.label, style = ValueStyle)
                        Text(text = describe(target), style = NoteStyle)
                    }
                    if (!installed) Text(text = "MISSING", style = NoteStyle.copy(color = Accent))
                }
            }
        }
    }

    item {
        Panel(onClick = { onToggleSearch(!showInSearch) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ring apps also in search", style = ValueStyle, modifier = Modifier.weight(1f))
                Text(
                    text = if (showInSearch) "ON" else "OFF",
                    style = ValueStyle.copy(color = if (showInSearch) Accent else NoteStyle.color),
                )
            }
        }
    }
}

/** What each swipe up runs, and a way to change it. */
internal fun LazyListScope.gestureShortcutRows(
    bindings: Map<ShortcutGesture, BoundShortcut>,
    onBind: (ShortcutGesture) -> Unit,
) {
    items(ShortcutGesture.entries.toList()) { gesture ->
        val bound = bindings[gesture]

        Panel(onClick = { onBind(gesture) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = gesture.label, style = ValueStyle)
                    Text(
                        text = bound?.let { "${it.appLabel}, ${it.label}" } ?: "Nothing bound",
                        style = NoteStyle,
                    )
                }
                Text(text = "BIND", style = ValueStyle.copy(color = Accent))
            }
        }
    }
}

/** A pinned web app has no package, so its icon comes from the repo rather than the system. */
@Composable
private fun RingTargetIcon(target: RingTarget) {
    val bundled = (target as? RingTarget.Web)?.icon

    if (bundled == null) {
        AppIcon(packageName = target.iconPackage, size = 30.dp)
    } else {
        Image(
            painter = painterResource(bundled),
            contentDescription = target.label,
            modifier = Modifier.size(30.dp),
        )
    }
}

private fun describe(target: RingTarget) = when (target) {
    is RingTarget.App -> target.packageName
    is RingTarget.Web -> "${target.url}, opened in ${target.browserPackage}"
}

internal val Compass = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/**
 * Every letter's shape, drawn from the templates actually in use.
 *
 * The first sample of each letter stands for it. Showing all five would be showing the tolerance
 * rather than the shape, and the shape is what someone reading this needs to copy.
 */
@Composable
internal fun AlphabetChart() {
    val context = LocalContext.current

    val samples by produceState(initialValue = emptyList<GraffitiSample>(), context) {
        val bundled = GraffitiStore.bundled(context).groupBy { it.letter }
        val captured = GraffitiStore.load(context).groupBy { it.letter }
        value = (bundled + captured).values.mapNotNull { it.firstOrNull() }
    }

    val byLetter = samples.associateBy { it.letter }

    Panel {
        GraffitiLetters.chunked(ChartColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { letter ->
                    LetterCell(letter = letter, sample = byLetter[letter], modifier = Modifier.weight(1f))
                }
                repeat(ChartColumns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LetterCell(letter: Char, sample: GraffitiSample?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sample == null) {
                Text(text = "?", style = NoteStyle)
            } else {
                GraffitiStrokeArt(points = sample.points, modifier = Modifier.fillMaxSize())
            }
        }
        Text(text = letter.uppercase(), style = NoteStyle)
    }
}

private const val ChartColumns = 6

/**
 * The contextual weights, as they are written in source.
 *
 * Reported rather than computed: this is the rule set, not a snapshot of what the ring would show
 * right now, which changes with every launch and would be a different screen entirely.
 */
internal fun LazyListScope.contextualRuleRows(entries: List<AppEntry>) {
    val name: (String) -> String = { packageName ->
        entries.firstOrNull { it.packageName == packageName }?.label ?: packageName
    }

    pullRows(name)
    trustRows(name)
    baselineRows(name)
    rampRows(name)
    reconciliationRows(name)
    notificationRows(name)
}

private fun LazyListScope.pullRows(name: (String) -> String) {
    item { Subheading("PULLS") }
    items(ContextualRules.pulls) { pull ->
        KeyValueWeightedRow(
            key = if (pull.source == pull.target) {
                "${name(pull.source)}, itself"
            } else {
                "${name(pull.source)} to ${name(pull.target)}"
            },
            weight = pull.weight,
            trailing = short(pull.window),
        )
    }
}

private fun LazyListScope.trustRows(name: (String) -> String) {
    item { Subheading("TRUST, SPLIT AT ${short(ContextualRules.TrustShortVisit)}") }
    items(ContextualRules.trustShortPulls.toList()) { (target, weight) ->
        KeyValueWeighted("Short visit, ${name(target)}", weight)
    }
    items(ContextualRules.trustLongPulls.toList()) { (target, weight) ->
        KeyValueWeighted("Long visit, ${name(target)}", weight)
    }
}

/**
 * Read through the rules rather than retyped. This panel exists to report them, and a copy of the
 * numbers would let it go on reporting the old ones after a change.
 */
private fun LazyListScope.baselineRows(name: (String) -> String) {
    item { Subheading("BASELINES AND MOTION") }

    items(listOf(ContextApps.PAYLAH, ContextApps.TRUST, ContextApps.WALLET)) { app ->
        val outside = ContextualRules.baseline(app, MotionState.STILL)
        val driving = ContextualRules.baseline(app, MotionState.IN_VEHICLE)

        if (outside == driving) {
            KeyValueWeighted("${name(app)}, always", outside)
        } else {
            Column {
                KeyValueWeighted("${name(app)}, not in a vehicle", outside)
                KeyValueWeighted("${name(app)}, in a vehicle", driving)
            }
        }
    }

    item {
        KeyValueWeighted(
            "Transit apps, walking",
            ContextualRules.motionBoost(ContextApps.MAPS, MotionState.WALKING),
        )
    }
    item {
        KeyValueWeighted(
            "${name(ContextApps.MAPS)}, in a vehicle",
            ContextualRules.motionBoost(ContextApps.MAPS, MotionState.IN_VEHICLE),
        )
    }
    item {
        KeyValueWeighted(
            "Other transit apps, in a vehicle",
            ContextualRules.motionBoost(ContextApps.GRAB, MotionState.IN_VEHICLE),
        )
    }
}

private fun LazyListScope.rampRows(name: (String) -> String) {
    item { Subheading("DORMANCY RAMPS") }
    items(ContextualRules.ramps) { ramp ->
        Column {
            KeyValueWeighted("${name(ramp.packageName)}, before day ${ramp.startDay}", ramp.floor)
            KeyValueWeighted("${name(ramp.packageName)}, from day ${ramp.peakDay}", ramp.peakScore)
        }
    }
}

private fun LazyListScope.reconciliationRows(name: (String) -> String) {
    item { Subheading("RECONCILIATION, FROM DAY ${ContextualRules.ReconciliationDay}") }
    items(ContextualRules.reconciliation.toList()) { (target, weight) ->
        KeyValueWeighted(name(target), weight)
    }
}

private fun LazyListScope.notificationRows(name: (String) -> String) {
    item { Subheading("NOTIFICATIONS AND USB") }
    item { KeyValueWeighted("Ongoing notification", ContextualRules.StickyNotificationBoost) }
    item {
        KeyValueWeighted(
            "Ordinary notification, apps a rule names",
            ContextualRules.RegularNotificationBoost,
        )
    }
    item {
        KeyValueWeighted(
            "${name(ContextualRules.UsbSettingsTarget)}, plugged into a computer",
            ContextualRules.UsbSettingsBoost,
        )
    }
}

@Composable
private fun Subheading(text: String) {
    Text(
        text = text,
        style = HeaderStyle.copy(color = NoteStyle.color),
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
    )
}

/** A weighted row that also carries the window the weight applies for. */
@Composable
private fun KeyValueWeightedRow(key: String, weight: Int, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = key, style = BodyStyle, modifier = Modifier.weight(1f))
        Text(text = signed(weight), style = ValueStyle.copy(color = weightColour(weight)))
        Text(text = trailing, style = NoteStyle, modifier = Modifier.width(38.dp))
    }
}

@Composable
private fun KeyValueWeighted(key: String, weight: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = key, style = BodyStyle, modifier = Modifier.weight(1f))
        Text(text = signed(weight), style = ValueStyle.copy(color = weightColour(weight)))
    }
}

private fun signed(weight: Int) = if (weight > 0) "+$weight" else "$weight"

private fun weightColour(weight: Int) = if (weight < 0) Dead else Live

private fun short(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes < 60) "${minutes}m" else "${duration.toHours()}h"
}
