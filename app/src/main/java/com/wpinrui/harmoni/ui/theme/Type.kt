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
