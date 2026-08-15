package com.wpinrui.harmoni.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.wpinrui.harmoni.R

/**
 * Karla, the face the design mockups are drawn in. One variable font file covers every weight the
 * launcher uses, from the extra light clock down to the medium labels.
 */
val Karla = FontFamily(
    karla(FontWeight.ExtraLight),
    karla(FontWeight.Light),
    karla(FontWeight.Normal),
    karla(FontWeight.Medium),
)

private fun karla(weight: FontWeight) = Font(
    resId = R.font.karla,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/**
 * The turned v, U+028C, which the wordmark sets in place of an A.
 *
 * Karla does not have the glyph: the shipped file and the upstream variable font both carry 393
 * codepoints and it is in neither. The mockups declare `Karla, system-ui, sans-serif`, so what
 * they render is the browser's fallback. This is that fallback made deliberate, subset from Inter
 * to the single glyph it is needed for.
 */
/**
 * Half a step below ExtraLight, which is as fine as the wordmark asks for.
 *
 * Inter runs a touch heavier than Karla at the same nominal weight, and its axis goes down to 100.
 */
val TurnedVWeight = FontWeight(150)

val TurnedV = FontFamily(
    turnedV(TurnedVWeight),
    turnedV(FontWeight.ExtraLight),
    turnedV(FontWeight.Light),
    turnedV(FontWeight.Normal),
)

private fun turnedV(weight: FontWeight) = Font(
    resId = R.font.inter_turned_v,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)
