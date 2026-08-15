package com.wpinrui.harmoni.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.context.ContextApps
import com.wpinrui.harmoni.context.ContextualRules
import com.wpinrui.harmoni.graffiti.GraffitiLetters
import com.wpinrui.harmoni.graffiti.GraffitiSample
import com.wpinrui.harmoni.graffiti.GraffitiStore
import com.wpinrui.harmoni.graffiti.GraffitiStrokeArt
import com.wpinrui.harmoni.home.RingBindings
import com.wpinrui.harmoni.home.RingTarget
import com.wpinrui.harmoni.home.iconPackage
import java.time.Duration

/**
 * The eight fixed positions, in the order the ring lays them out.
 *
 * Position leads the row rather than being implied by order: the ring is muscle memory, and
 * remembering which way is which is usually the reason to open this screen.
 */
internal fun LazyListScope.ringBindingRows(entries: List<AppEntry>) {
    itemsIndexed(RingBindings.slots) { index, target ->
        val installed = entries.any { it.packageName == target.iconPackage }

        Row2(
            key = "${Compass[index]}  ${target.label}",
            value = if (installed) describe(target) else "NOT INSTALLED",
            valueColour = if (installed) Muted else Dead,
        )
    }
}

private fun describe(target: RingTarget) = when (target) {
    is RingTarget.App -> target.packageName
    is RingTarget.Web -> target.url
}

private val Compass = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/**
 * Every letter's shape, drawn from the templates actually in use.
 *
 * The first sample of each letter stands for it. Showing all five would show the tolerance rather
 * than the shape, and the shape is what someone reading this has to copy.
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

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        GraffitiLetters.chunked(ChartColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sample == null) {
                Text(text = "?", style = RowStyle.copy(color = Muted))
            } else {
                GraffitiStrokeArt(points = sample.points, modifier = Modifier.fillMaxSize(), colour = Ink)
            }
        }
        Text(text = letter.uppercase(), style = RowStyle.copy(color = Muted))
    }
}

private const val ChartColumns = 6

/**
 * The weights as they are written in source, not a snapshot of what the ring would pick now, which
 * changes with every launch and is a different screen.
 */
internal fun LazyListScope.contextualRuleRows(entries: List<AppEntry>) {
    val name: (String) -> String = { packageName ->
        entries.firstOrNull { it.packageName == packageName }?.label ?: packageName
    }

    item { Subheading("PULLS") }
    items(ContextualRules.pulls) { pull ->
        Row2(
            key = if (pull.source == pull.target) {
                "${name(pull.source)}, itself"
            } else {
                "${name(pull.source)} to ${name(pull.target)}"
            },
            value = "${signed(pull.weight)}   ${short(pull.window)}",
            valueColour = weightColour(pull.weight),
        )
    }

    item { Subheading("TRUST, SPLIT AT ${short(ContextualRules.TrustShortVisit)}") }
    items(ContextualRules.trustShortPulls.toList()) { (target, weight) ->
        Weighted("Short visit, ${name(target)}", weight)
    }
    items(ContextualRules.trustLongPulls.toList()) { (target, weight) ->
        Weighted("Long visit, ${name(target)}", weight)
    }

    item { Subheading("BASELINES AND MOTION") }
    item { Weighted("${name(ContextApps.PAYLAH)}, still", 55) }
    item { Weighted("${name(ContextApps.TRUST)}, still", 35) }
    item { Weighted("${name(ContextApps.WALLET)}, always", 20) }
    item { Weighted("Transit apps, walking", 10) }
    item { Weighted("${name(ContextApps.MAPS)}, in a vehicle", 45) }
    item { Weighted("Other transit apps, in a vehicle", 40) }

    item { Subheading("DORMANCY RAMPS") }
    ContextualRules.ramps.forEach { ramp ->
        item { Weighted("${name(ramp.packageName)}, before day ${ramp.startDay}", ramp.floor) }
        item { Weighted("${name(ramp.packageName)}, from day ${ramp.peakDay}", ramp.peakScore) }
    }

    item { Subheading("RECONCILIATION, FROM DAY ${ContextualRules.ReconciliationDay}") }
    items(ContextualRules.reconciliation.toList()) { (target, weight) ->
        Weighted(name(target), weight)
    }

    item { Subheading("NOTIFICATIONS AND USB") }
    item { Weighted("Ongoing notification", ContextualRules.StickyNotificationBoost) }
    item { Weighted("Ordinary notification, apps a rule names", ContextualRules.RegularNotificationBoost) }
    item {
        Weighted(
            "${name(ContextualRules.UsbSettingsTarget)}, plugged into a computer",
            ContextualRules.UsbSettingsBoost,
        )
    }
}

@Composable
private fun Subheading(text: String) {
    Text(
        text = text,
        style = TitleStyle.copy(color = Muted),
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
    )
}

@Composable
private fun Weighted(key: String, weight: Int) =
    Row2(key = key, value = signed(weight), valueColour = weightColour(weight))

private fun signed(weight: Int) = if (weight > 0) "+$weight" else "$weight"

private fun weightColour(weight: Int) = if (weight < 0) Dead else Live

private fun short(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes < 60) "${minutes}m" else "${duration.toHours()}h"
}
