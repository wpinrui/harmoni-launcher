package com.wpinrui.harmoni.app

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wpinrui.harmoni.apps.AppEntry
import com.wpinrui.harmoni.apps.AppIcon
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
 * Position is the binding, so it is named rather than implied: the ring is muscle memory, and the
 * reason to read this screen is usually to remember which way is which.
 */
internal fun LazyListScope.ringBindingRows(entries: List<AppEntry>) {
    itemsIndexed(RingBindings.slots) { index, target ->
        val installed = entries.any { it.packageName == target.iconPackage }

        Panel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = Compass[index],
                    style = ValueStyle.copy(color = Accent, textAlign = TextAlign.Center),
                    modifier = Modifier.width(30.dp),
                )
                AppIcon(packageName = target.iconPackage, size = 30.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = target.label, style = ValueStyle)
                    Text(text = describe(target), style = NoteStyle)
                }
                if (!installed) Text(text = "MISSING", style = NoteStyle.copy(color = Accent))
            }
        }
    }
}

private fun describe(target: RingTarget) = when (target) {
    is RingTarget.App -> target.packageName
    is RingTarget.Web -> "${target.url}, opened in ${target.browserPackage}"
}

private val Compass = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

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

    item {
        Panel {
            Text(text = "PULLS", style = HeaderStyle, modifier = Modifier.padding(bottom = 6.dp))
            ContextualRules.pulls.forEach { pull ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (pull.source == pull.target) {
                            "${name(pull.source)}, itself"
                        } else {
                            "${name(pull.source)} to ${name(pull.target)}"
                        },
                        style = BodyStyle,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = signed(pull.weight), style = ValueStyle.copy(color = weightColour(pull.weight)))
                    Text(text = short(pull.window), style = NoteStyle, modifier = Modifier.width(38.dp))
                }
            }
        }
    }

    item {
        Panel {
            Text(
                text = "TRUST, SPLIT AT ${short(ContextualRules.TrustShortVisit)}",
                style = HeaderStyle,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            ContextualRules.trustShortPulls.forEach { (target, weight) ->
                KeyValueWeighted("Short visit, ${name(target)}", weight)
            }
            ContextualRules.trustLongPulls.forEach { (target, weight) ->
                KeyValueWeighted("Long visit, ${name(target)}", weight)
            }
        }
    }

    item {
        Panel {
            Text(
                text = "BASELINES AND MOTION",
                style = HeaderStyle,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            KeyValueWeighted("${name(ContextApps.PAYLAH)}, still", 55)
            KeyValueWeighted("${name(ContextApps.TRUST)}, still", 35)
            KeyValueWeighted("${name(ContextApps.WALLET)}, always", 20)
            KeyValueWeighted("Transit apps, walking", 10)
            KeyValueWeighted("${name(ContextApps.MAPS)}, in a vehicle", 45)
            KeyValueWeighted("Other transit apps, in a vehicle", 40)
        }
    }

    item {
        Panel {
            Text(text = "DORMANCY RAMPS", style = HeaderStyle, modifier = Modifier.padding(bottom = 6.dp))
            ContextualRules.ramps.forEach { ramp ->
                Text(text = name(ramp.packageName), style = ValueStyle)
                KeyValueWeighted("Before day ${ramp.startDay}", ramp.floor)
                KeyValueWeighted("From day ${ramp.peakDay}", ramp.peakScore)
            }
        }
    }

    item {
        Panel {
            Text(
                text = "RECONCILIATION, FROM DAY ${ContextualRules.ReconciliationDay}",
                style = HeaderStyle,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            ContextualRules.reconciliation.forEach { (target, weight) ->
                KeyValueWeighted(name(target), weight)
            }
            KeyValueWeighted(
                "${name(ContextualRules.UsbSettingsTarget)}, plugged into a computer",
                ContextualRules.UsbSettingsBoost,
            )
            KeyValueWeighted("Ongoing notification", ContextualRules.StickyNotificationBoost)
            KeyValueWeighted("Ordinary notification, apps a rule already names", ContextualRules.RegularNotificationBoost)
        }
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

private fun weightColour(weight: Int) = if (weight < 0) Color(0xFFCE8B7F) else Color(0xFF9FC9A6)

private fun short(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes < 60) "${minutes}m" else "${duration.toHours()}h"
}
