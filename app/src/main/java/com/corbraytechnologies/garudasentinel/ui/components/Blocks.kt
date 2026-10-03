package com.corbraytechnologies.garudasentinel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.corbraytechnologies.garudasentinel.ui.theme.GarudaSentinelTheme
import com.corbraytechnologies.garudasentinel.ui.theme.GarudaType
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.ui.theme.SmallCorner

// Building blocks for the redesigned screens: sections separated by hairline dividers rather
// than nested cards. Every block uses the same 24dp side margin.

/** Side margin shared by all blocks in this file. */
val ScreenGutter: Dp = 24.dp

/** Newsreader title at the top of a screen, marked as a heading for TalkBack. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = GarudaType.ScreenTitle,
        color = Palette.Text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/** Monospace caps label that opens a section, with the standard space above and below. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.toUpperCase(LocaleList.current),
        style = GarudaType.SectionLabel,
        color = Palette.TextMuted,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, top = 32.dp, bottom = 12.dp)
            .semantics { heading() },
    )
}

/** 1dp line in the hairline colour. Full bleed by default; [inset] indents it to the text edge on both sides. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier, inset: Boolean = false) {
    HorizontalDivider(
        modifier = if (inset) modifier.padding(horizontal = ScreenGutter) else modifier,
        thickness = 1.dp,
        color = Palette.Hairline,
    )
}

/**
 * One line of a list: title, optional supporting line, optional right-aligned monospace value
 * and optional chevron. When [onClick] is set the whole row is clickable, with a ripple.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = ScreenGutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
            }
        }
        value?.let {
            Text(
                it,
                style = GarudaType.NumberMedium,
                color = Palette.TextDim,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Palette.TextMuted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** How much a finding matters. The label is shown in capitals above the headline. */
enum class Severity(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    OK("OK"),
    ;

    val color get() = when (this) {
        HIGH -> Palette.SeverityHigh
        MEDIUM -> Palette.SeverityMedium
        LOW -> Palette.SeverityLow
        OK -> Palette.Ok
    }
}

/**
 * One finding: severity, headline, body and an optional action. High findings carry a 3dp
 * accent bar on the left; the text stays aligned with the rest of the screen either way.
 */
@Composable
fun FindingRow(
    severity: Severity,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val barWidth = 3.dp
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        if (severity == Severity.HIGH) {
            Box(Modifier.width(barWidth).fillMaxHeight().background(Palette.SeverityHigh))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(
                    start = if (severity == Severity.HIGH) ScreenGutter - barWidth else ScreenGutter,
                    end = ScreenGutter,
                    top = 16.dp,
                    bottom = if (actionLabel != null) 4.dp else 16.dp,
                ),
        ) {
            Text(
                severity.label.toUpperCase(LocaleList.current),
                style = GarudaType.SectionLabel,
                color = severity.color,
            )
            Spacer(Modifier.height(6.dp))
            Text(headline, style = MaterialTheme.typography.titleMedium, color = Palette.Text)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
            if (actionLabel != null) {
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onAction),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = Palette.Accent)
                }
            }
        }
    }
}

/** A number and its caption, for [StatGroup]. */
data class Stat(val value: String, val caption: String)

/** Two to four monospace numbers with small captions, side by side in equal columns. */
@Composable
fun StatGroup(stats: List<Stat>, modifier: Modifier = Modifier) {
    require(stats.size in 2..4) { "StatGroup shows two to four numbers, got ${stats.size}." }
    Row(
        modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        stats.forEach { stat ->
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(stat.value, style = GarudaType.NumberLarge, color = Palette.Text, maxLines = 1)
                Text(stat.caption, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
        }
    }
}

private val ButtonHeight = 50.dp

/** The main action on a screen: gold, 50dp high, full width unless [fullWidth] is false. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SmallCorner,
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Accent,
            contentColor = Palette.OnAccent,
            disabledContainerColor = Palette.Hairline,
            disabledContentColor = Palette.TextMuted,
        ),
        modifier = modifier.height(ButtonHeight).then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** A secondary action: outlined with a hairline border, 50dp high, full width unless [fullWidth] is false. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = SmallCorner,
        border = BorderStroke(1.dp, Palette.Hairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Palette.Text,
            disabledContentColor = Palette.TextMuted,
        ),
        modifier = modifier.height(ButtonHeight).then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    GarudaSentinelTheme {
        Surface(color = Palette.Background) {
            Column { content() }
        }
    }
}

@Preview(name = "SectionLabel")
@Composable
private fun SectionLabelPreview() = PreviewFrame { SectionLabel("On this device now") }

@Preview(name = "HairlineDivider")
@Composable
private fun HairlineDividerPreview() = PreviewFrame {
    ListRow("Full bleed below")
    HairlineDivider()
    ListRow("Inset below")
    HairlineDivider(inset = true)
    ListRow("Last row")
}

@Preview(name = "ListRow")
@Composable
private fun ListRowPreview() = PreviewFrame {
    ListRow("Apps", supporting = "Installed by you", value = "42", showChevron = true, onClick = {})
    HairlineDivider(inset = true)
    ListRow("Android version", value = "16")
    HairlineDivider(inset = true)
    ListRow("Photos with location", supporting = "Taken on this phone")
}

@Preview(name = "FindingRow")
@Composable
private fun FindingRowPreview() = PreviewFrame {
    FindingRow(Severity.HIGH, "An app can read your notifications", "Example Tracker has notification access.", actionLabel = "Review in Settings")
    HairlineDivider()
    FindingRow(Severity.MEDIUM, "Installed from outside a store", "Two apps came from a file.")
    HairlineDivider()
    FindingRow(Severity.LOW, "Developer options are on", "Nothing else points to a problem.")
    HairlineDivider()
    FindingRow(Severity.OK, "Screen lock is on", "The phone asks for a PIN or fingerprint.")
}

@Preview(name = "StatGroup")
@Composable
private fun StatGroupPreview() = PreviewFrame {
    StatGroup(listOf(Stat("257", "apps"), Stat("1,204", "photos"), Stat("6", "files")))
    HairlineDivider()
    StatGroup(listOf(Stat("3h 12m", "today"), Stat("41", "opens")))
}

@Preview(name = "Buttons")
@Composable
private fun ButtonsPreview() = PreviewFrame {
    Column(Modifier.padding(ScreenGutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton("Start scan", onClick = {})
        SecondaryButton("Export to a file", onClick = {})
        PrimaryButton("Disabled", onClick = {}, enabled = false)
        SecondaryButton("Not full width", onClick = {}, fullWidth = false)
    }
}
