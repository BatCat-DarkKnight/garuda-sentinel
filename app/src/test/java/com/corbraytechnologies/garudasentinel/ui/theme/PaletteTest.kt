package com.corbraytechnologies.garudasentinel.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class PaletteTest {

    private val tokens = setOf(
        Palette.Background, Palette.Surface, Palette.Hairline,
        Palette.Text, Palette.TextMuted, Palette.TextDim,
        Palette.Accent, Palette.OnAccent,
        Palette.SeverityHigh, Palette.SeverityMedium, Palette.SeverityLow, Palette.Ok,
        Palette.AccentTint, Palette.SeverityHighTint, Palette.OkTint,
    )

    @Test
    fun everyMaterialRoleIsAPaletteToken() {
        val s = GarudaColorScheme
        val roles = mapOf(
            "primary" to s.primary, "onPrimary" to s.onPrimary,
            "primaryContainer" to s.primaryContainer, "onPrimaryContainer" to s.onPrimaryContainer,
            "inversePrimary" to s.inversePrimary,
            "secondary" to s.secondary, "onSecondary" to s.onSecondary,
            "secondaryContainer" to s.secondaryContainer, "onSecondaryContainer" to s.onSecondaryContainer,
            "tertiary" to s.tertiary, "onTertiary" to s.onTertiary,
            "tertiaryContainer" to s.tertiaryContainer, "onTertiaryContainer" to s.onTertiaryContainer,
            "background" to s.background, "onBackground" to s.onBackground,
            "surface" to s.surface, "onSurface" to s.onSurface,
            "surfaceVariant" to s.surfaceVariant, "onSurfaceVariant" to s.onSurfaceVariant,
            "surfaceTint" to s.surfaceTint,
            "inverseSurface" to s.inverseSurface, "inverseOnSurface" to s.inverseOnSurface,
            "error" to s.error, "onError" to s.onError,
            "errorContainer" to s.errorContainer, "onErrorContainer" to s.onErrorContainer,
            "outline" to s.outline, "outlineVariant" to s.outlineVariant, "scrim" to s.scrim,
            "surfaceBright" to s.surfaceBright, "surfaceDim" to s.surfaceDim,
            "surfaceContainerLowest" to s.surfaceContainerLowest, "surfaceContainerLow" to s.surfaceContainerLow,
            "surfaceContainer" to s.surfaceContainer, "surfaceContainerHigh" to s.surfaceContainerHigh,
            "surfaceContainerHighest" to s.surfaceContainerHighest,
        )
        val strays = roles.filterValues { it !in tokens }
        assertTrue("Roles not taken from the palette: ${strays.keys}", strays.isEmpty())
    }

    /** WCAG 2.x AA: 4.5:1 for body text, 3:1 for text at 24sp or larger. */
    @Test
    fun textPairsMeetWcagAa() {
        val texts = mapOf(
            "text" to Palette.Text, "textMuted" to Palette.TextMuted, "textDim" to Palette.TextDim,
            "accent" to Palette.Accent, "severityHigh" to Palette.SeverityHigh,
            "severityMedium" to Palette.SeverityMedium, "severityLow" to Palette.SeverityLow, "ok" to Palette.Ok,
        )
        val backgrounds = mapOf(
            "background" to Palette.Background, "surface" to Palette.Surface, "hairline" to Palette.Hairline,
        )
        val pairs = texts.flatMap { (t, fg) -> backgrounds.map { (b, bg) -> "$t on $b" to (fg to bg) } } + listOf(
            "onAccent on accent" to (Palette.OnAccent to Palette.Accent),
            "accent on accentTint" to (Palette.Accent to Palette.AccentTint),
            "text on accentTint" to (Palette.Text to Palette.AccentTint),
            "textMuted on accentTint" to (Palette.TextMuted to Palette.AccentTint),
            "severityHigh on severityHighTint" to (Palette.SeverityHigh to Palette.SeverityHighTint),
            "ok on okTint" to (Palette.Ok to Palette.OkTint),
            "background on severityHigh" to (Palette.Background to Palette.SeverityHigh),
        )
        val failing = pairs.filter { (_, c) -> contrast(c.first, c.second) < 4.5 }.map { it.first }
        assertTrue("Below 4.5:1: $failing", failing.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
