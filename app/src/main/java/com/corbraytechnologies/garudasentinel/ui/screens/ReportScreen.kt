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

package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.findings.ChangeLine
import com.corbraytechnologies.garudasentinel.findings.FindingAction
import com.corbraytechnologies.garudasentinel.findings.FindingSeverity
import com.corbraytechnologies.garudasentinel.findings.Report
import com.corbraytechnologies.garudasentinel.findings.ReportText
import com.corbraytechnologies.garudasentinel.scan.ScanState
import com.corbraytechnologies.garudasentinel.scan.StepStatus
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.appRoute
import com.corbraytechnologies.garudasentinel.ui.appsByAccessRoute
import com.corbraytechnologies.garudasentinel.ui.components.FindingRow
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ListRow
import com.corbraytechnologies.garudasentinel.ui.components.PrimaryButton
import com.corbraytechnologies.garudasentinel.ui.components.ScreenGutter
import com.corbraytechnologies.garudasentinel.ui.components.SectionLabel
import com.corbraytechnologies.garudasentinel.ui.components.Severity
import com.corbraytechnologies.garudasentinel.ui.components.appInfoIntent
import com.corbraytechnologies.garudasentinel.ui.components.intent
import com.corbraytechnologies.garudasentinel.ui.components.openSettings
import com.corbraytechnologies.garudasentinel.ui.locatedPhotosRoute
import com.corbraytechnologies.garudasentinel.ui.theme.GarudaType
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.utils.countOf

/**
 * The start screen: a ranked list of what is worth a look, each with one sentence and one
 * action, then the data sources under "Your data".
 */
@Composable
fun ReportScreen(main: MainViewModel, onNavigate: (Dest) -> Unit, onRoute: (String) -> Unit) {
    val context = LocalContext.current
    val scan by main.scanState.collectAsStateWithLifecycle()
    val hasScanned by main.hasScanned.collectAsStateWithLifecycle()
    val report by main.report.collectAsStateWithLifecycle()
    val changes by main.changes.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        main.refreshChecks()
        onPauseOrDispose { }
    }

    val onAction: (FindingAction) -> Unit = { action ->
        when (action) {
            is FindingAction.ReviewApp -> onRoute(appRoute(action.packageName))
            is FindingAction.OpenSetting -> context.openSettings(action.target.intent())
            is FindingAction.OpenAppInfo -> context.openSettings(appInfoIntent(action.packageName))
            FindingAction.SeePlaces -> onRoute(locatedPhotosRoute())
            FindingAction.SeeAppsByAccess -> onRoute(appsByAccessRoute())
            FindingAction.SetUp -> onNavigate(Dest.PERMISSIONS)
            FindingAction.SeeWhoCanWatch -> onNavigate(Dest.WATCHERS)
        }
    }

    // Wait for the database before choosing between "first check" and the report.
    val scanned = hasScanned ?: return
    val current = report

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "GARUDA SENTINEL",
            style = GarudaType.Eyebrow,
            color = Palette.TextMuted,
            modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 20.dp, bottom = 14.dp),
        )
        HairlineDivider()

        Text(
            ReportText.headline(current, scanned),
            style = GarudaType.ReportHeadline,
            color = Palette.Text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ScreenGutter, end = ScreenGutter, top = 28.dp)
                .semantics { heading() },
        )
        if (scanned && current != null) {
            Spacer(Modifier.height(12.dp))
            Tally(current)
        }
        Spacer(Modifier.height(24.dp))
        ScanButton(scan, scanned, onStart = main::startScan, onCancel = main::cancelScan)
        Spacer(Modifier.height(8.dp))

        if (!scanned) return@Column

        val since = changes
        if (since != null && !main.memoryOnly) {
            SectionLabel(ReportText.sinceLabel(since.previousAt))
            val lines = ReportText.changeLines(since)
            if (lines.isEmpty()) {
                Text(
                    "Nothing changed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextMuted,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
            }
            lines.forEach { ChangeRow(it) }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))
        HairlineDivider()
        if (current == null) {
            Text(
                "Reading settings...",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextMuted,
                modifier = Modifier.padding(ScreenGutter),
            )
        } else {
            current.findings.forEach { finding ->
                FindingRow(
                    severity = finding.severity.toUi(),
                    headline = finding.title,
                    body = finding.sentence,
                    actionLabel = finding.action.label,
                    onAction = { onAction(finding.action) },
                )
                HairlineDivider()
            }
            current.notChecked.forEach { row ->
                FindingRow(Severity.NOT_CHECKED, row.title, row.sentence, actionLabel = row.action.label, onAction = { onAction(row.action) })
                HairlineDivider()
            }
            ListRow(
                countOf(current.passed, "check") + " passed",
                titleColor = Palette.Ok,
                showChevron = true,
                onClick = { onNavigate(Dest.WATCHERS) },
            )
        }

        SectionLabel("Your data")
        YourDataRows(main, onNavigate)
        Spacer(Modifier.height(24.dp))
    }
}

private fun FindingSeverity.toUi(): Severity = when (this) {
    FindingSeverity.HIGH -> Severity.HIGH
    FindingSeverity.MEDIUM -> Severity.MEDIUM
    FindingSeverity.LOW -> Severity.LOW
}

/** "1 high · 2 medium · 7 passed", each count in its severity colour. */
@Composable
private fun Tally(report: Report) {
    val parts = ReportText.tally(report)
    val text = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index > 0) withStyle(SpanStyle(color = Palette.TextMuted)) { append(" · ") }
            val color = when (part.severity) {
                FindingSeverity.HIGH -> Palette.SeverityHigh
                FindingSeverity.MEDIUM -> Palette.SeverityMedium
                FindingSeverity.LOW -> Palette.SeverityLow
                null -> Palette.Ok
            }
            withStyle(SpanStyle(color = color)) { append(part.text) }
        }
    }
    Text(text, style = GarudaType.Tally, modifier = Modifier.padding(horizontal = ScreenGutter))
}

/** "Run a check" or "Check again"; while a scan runs it shows its progress and stops the scan when tapped. */
@Composable
private fun ScanButton(scan: ScanState, scanned: Boolean, onStart: () -> Unit, onCancel: () -> Unit) {
    val modifier = Modifier.padding(horizontal = ScreenGutter)
    if (scan.running) {
        val done = scan.steps.values.count { it !is StepStatus.Waiting && it !is StepStatus.Running }
        PrimaryButton(
            "Checking $done of ${scan.steps.size}. Tap to stop",
            onClick = onCancel,
            modifier = modifier,
            leading = { CircularProgressIndicator(Modifier.size(18.dp), color = Palette.OnAccent, strokeWidth = 2.dp) },
        )
    } else {
        PrimaryButton(if (scanned) "Check again" else "Run a check", onClick = onStart, modifier = modifier)
    }
}

/** One "since your last check" line: a gold plus for something added, a grey minus sign for something removed. */
@Composable
private fun ChangeRow(line: ChangeLine) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            if (line.added) "+ " else "− ",
            style = GarudaType.NumberMedium,
            color = if (line.added) Palette.Accent else Palette.TextMuted,
        )
        Text(line.text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
    }
}
