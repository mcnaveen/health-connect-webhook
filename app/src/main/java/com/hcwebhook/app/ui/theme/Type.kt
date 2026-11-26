@file:OptIn(ExperimentalTextApi::class)

package com.hcwebhook.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.hcwebhook.app.R

/**
 * Bricolage Grotesque, for screen titles, the status card title and its numbers. Text in
 * scripts that the font does not have (Tamil, Arabic, CJK, Korean) falls back to the system font.
 */
val Display = FontFamily(
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("opsz", 32f),
        ),
    ),
)

/** IBM Plex Mono, for data: URLs, HTTP codes, timestamps and counts. */
val Mono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

private val Base = Typography()

// Only the large styles change: body text stays on the system font for every language.
val Typography = Base.copy(
    headlineSmall = Base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
)
