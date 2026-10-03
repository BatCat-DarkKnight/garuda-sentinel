package com.corbraytechnologies.garudasentinel.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * The app's colours. Screens and components use these tokens (or the Material roles mapped
 * from them in [GarudaSentinelTheme]), never loose hex values.
 */
object Palette {
    val Background = Color(0xFF0B1017)
    val Surface = Color(0xFF10161F)
    val Hairline = Color(0xFF1D2735)

    /** 1dp outline of secondary buttons and text fields. */
    val OutlineStrong = Color(0xFF2A3648)

    /** 1dp outline of destructive buttons. */
    val DangerOutline = Color(0xFF5E3038)

    val Text = Color(0xFFE9EEF3)
    val TextMuted = Color(0xFF8D99A8)
    val TextDim = Color(0xFFA9B4C2)

    /** Gold. */
    val Accent = Color(0xFFE3AE3C)
    val OnAccent = Color(0xFF14100A)

    val SeverityHigh = Color(0xFFF08C7A)
    val SeverityMedium = Color(0xFFE0A44E)
    val SeverityLow = Color(0xFF8D99A8)
    val Ok = Color(0xFF58C0A0)

    // Quiet containers for selected and highlighted states, made from the tokens above so no
    // other hue can creep in.
    val AccentTint = Accent.copy(alpha = TINT_ALPHA).compositeOver(Background)
    val SeverityHighTint = SeverityHigh.copy(alpha = TINT_ALPHA).compositeOver(Background)
    val OkTint = Ok.copy(alpha = TINT_ALPHA).compositeOver(Background)
}

private const val TINT_ALPHA = 0.14f
