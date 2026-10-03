package com.corbraytechnologies.garudasentinel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.corbraytechnologies.garudasentinel.R

// All fonts ship in res/font; the app has no network access, so nothing is downloaded.
// Sources, licences and how the files were made: docs/fonts/README.md.

/** Newsreader, for display text, headlines and large titles. */
val NewsreaderFontFamily = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
)

/** IBM Plex Sans, for body text and UI. One variable font file, with each weight selected by its variation setting. */
val PlexSansFontFamily = FontFamily(
    plexSans(FontWeight.Normal),
    plexSans(FontWeight.Medium),
    plexSans(FontWeight.SemiBold),
)

@OptIn(ExperimentalTextApi::class)
private fun plexSans(weight: FontWeight) =
    Font(R.font.ibm_plex_sans, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** IBM Plex Mono, for numbers, section labels and technical values. */
val PlexMonoFontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

private fun newsreader(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = NewsreaderFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

private fun plexSans(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = PlexSansFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Newsreader Medium for display, headlines and titleLarge; IBM Plex Sans for body, smaller
 * titles and labels. Monospace styles that Material has no slot for are in [GarudaType].
 */
val Typography = Typography(
    displayLarge = newsreader(42, 44),
    displayMedium = newsreader(36, 40),
    displaySmall = newsreader(33, 37),
    headlineLarge = newsreader(30, 34),
    headlineMedium = newsreader(27, 32),
    headlineSmall = newsreader(25, 30),
    titleLarge = newsreader(23, 29),
    titleMedium = plexSans(16, 24, FontWeight.Medium),
    titleSmall = plexSans(14, 20, FontWeight.Medium),
    bodyLarge = plexSans(16, 25, FontWeight.Normal),
    bodyMedium = plexSans(15, 23, FontWeight.Normal),
    bodySmall = plexSans(13, 19, FontWeight.Normal),
    labelLarge = plexSans(14, 20, FontWeight.Medium, letterSpacing = 0.1),
    labelMedium = plexSans(12, 16, FontWeight.Medium, letterSpacing = 0.2),
    labelSmall = plexSans(11, 16, FontWeight.Medium, letterSpacing = 0.3),
)

/** IBM Plex Mono styles for section labels and numbers. */
object GarudaType {
    /** Small caps section label. Set the text in capitals; the style does not change case. */
    val SectionLabel = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.16.em,
    )

    /** Headline numbers, such as the figures in a stat group. */
    val NumberLarge = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    )

    /** Values next to body text, such as the right-hand value in a list row. */
    val NumberMedium = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )

    /** Small technical values: package names, sizes, coordinates. */
    val NumberSmall = TextStyle(
        fontFamily = PlexMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
}
