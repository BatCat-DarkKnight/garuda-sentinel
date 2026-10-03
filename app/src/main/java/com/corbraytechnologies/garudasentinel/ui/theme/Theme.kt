package com.corbraytechnologies.garudasentinel.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Every Material role is set from [Palette]. Components such as the navigation drawer, filter
 * chips, switches, checkboxes and radio buttons take their colours from these roles, so none of
 * them falls back to Material's purple defaults.
 */
val GarudaColorScheme: ColorScheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnAccent,
    primaryContainer = Palette.AccentTint,
    onPrimaryContainer = Palette.Accent,
    inversePrimary = Palette.Accent,
    // Selected drawer items and selected filter chips use the secondary container.
    secondary = Palette.Accent,
    onSecondary = Palette.OnAccent,
    secondaryContainer = Palette.AccentTint,
    onSecondaryContainer = Palette.Accent,
    tertiary = Palette.Ok,
    onTertiary = Palette.Background,
    tertiaryContainer = Palette.OkTint,
    onTertiaryContainer = Palette.Ok,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Surface,
    onSurfaceVariant = Palette.TextDim,
    // No tonal tint on raised surfaces: tinting with the surface colour itself changes nothing.
    surfaceTint = Palette.Surface,
    inverseSurface = Palette.Text,
    inverseOnSurface = Palette.Background,
    error = Palette.SeverityHigh,
    onError = Palette.Background,
    errorContainer = Palette.SeverityHighTint,
    onErrorContainer = Palette.SeverityHigh,
    outline = Palette.TextMuted,
    outlineVariant = Palette.Hairline,
    scrim = Palette.Background,
    surfaceBright = Palette.Hairline,
    surfaceDim = Palette.Background,
    // The drawer uses surfaceContainerLow, dialogs surfaceContainerHigh, cards and the unchecked
    // switch track surfaceContainerHighest.
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.Surface,
    surfaceContainerHighest = Palette.Surface,
)

/** 3.dp for buttons, inputs and small surfaces, 4.dp for larger ones. No pill shapes. */
val GarudaShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

/** For buttons, inputs and small surfaces. Material's buttons default to a pill, so pass this. */
val SmallCorner = RoundedCornerShape(3.dp)

/** For larger surfaces such as sheets and dialogs. */
val LargeCorner = RoundedCornerShape(4.dp)

private val GarudaTextSelectionColors = TextSelectionColors(
    handleColor = Palette.Accent,
    backgroundColor = Palette.Accent.copy(alpha = 0.35f),
)

// Ripples take the colour of the content they sit on, which is always a palette colour.
// The alphas are raised a little from Material's, which are tuned for light surfaces.
@OptIn(ExperimentalMaterial3Api::class)
private val GarudaRipple = RippleConfiguration(
    color = Color.Unspecified,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.12f,
        hoveredAlpha = 0.08f,
        pressedAlpha = 0.14f,
    ),
)

/**
 * The app uses a single dark theme. Edge-to-edge is enabled in MainActivity, and
 * screens take their insets from Scaffold, so no status bar colors are set here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarudaSentinelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GarudaColorScheme,
        typography = Typography,
        shapes = GarudaShapes,
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides GarudaTextSelectionColors,
            LocalRippleConfiguration provides GarudaRipple,
            content = content,
        )
    }
}
