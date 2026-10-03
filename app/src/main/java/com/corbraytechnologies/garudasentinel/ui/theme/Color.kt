/*
 * Garuda Sentinel, a personal Android privacy tool.
 * Copyright (C) 2025-2026 Karl Corbray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
