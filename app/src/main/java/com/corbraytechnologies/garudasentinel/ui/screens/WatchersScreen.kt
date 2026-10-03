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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.findings.FindingRules
import com.corbraytechnologies.garudasentinel.findings.WatcherRows
import com.corbraytechnologies.garudasentinel.model.WatcherFinding
import com.corbraytechnologies.garudasentinel.model.WatcherKind
import com.corbraytechnologies.garudasentinel.model.WatcherLevel
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.appRoute
import com.corbraytechnologies.garudasentinel.ui.components.BarredBlock
import com.corbraytechnologies.garudasentinel.ui.components.CopyableAddress
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ListRow
import com.corbraytechnologies.garudasentinel.ui.components.PrimaryButton
import com.corbraytechnologies.garudasentinel.ui.components.ScreenGutter
import com.corbraytechnologies.garudasentinel.ui.components.ScreenTitle
import com.corbraytechnologies.garudasentinel.ui.components.SecondaryButton
import com.corbraytechnologies.garudasentinel.ui.components.SectionLabel
import com.corbraytechnologies.garudasentinel.ui.components.TextAction
import com.corbraytechnologies.garudasentinel.ui.components.appInfoIntent
import com.corbraytechnologies.garudasentinel.ui.components.intent
import com.corbraytechnologies.garudasentinel.ui.components.openSettings
import com.corbraytechnologies.garudasentinel.ui.theme.GarudaType
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.utils.WatcherRules

/** Shown as text with a copy button, never as a link. */
private const val SAFETY_ADDRESS = "stopstalkerware.org"

/**
 * Shows which apps and settings on this phone can watch the user. Everything here is read only;
 * every button opens an Android screen where the user decides.
 */
@Composable
fun WatchersScreen(main: MainViewModel, onBack: () -> Unit, onRoute: (String) -> Unit) {
    val apps by main.apps.collectAsStateWithLifecycle()
    val signals by main.signals.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        main.refreshChecks()
        onPauseOrDispose { }
    }

    fun openSetting(finding: WatcherFinding) {
        val single = finding.apps.singleOrNull()
        val target = FindingRules.settingsTarget(finding.kind) ?: return
        val appBased = finding.kind == WatcherKind.HIDDEN_APPS || finding.kind == WatcherKind.SIDELOADED
        context.openSettings(if (appBased && single != null) appInfoIntent(single.packageName) else target.intent())
    }

    GarudaScaffold(title = "", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ScreenTitle("Who can watch this phone")
            Text(
                "These are the settings that let software see what you do. Garuda Sentinel only reads them. " +
                    "It never changes anything.",
                style = MaterialTheme.typography.bodyLarge,
                color = Palette.TextDim,
                modifier = Modifier.padding(horizontal = ScreenGutter),
            )
            Spacer(Modifier.height(20.dp))
            SafetyNote()

            val current = signals
            when {
                apps.isEmpty() -> Note("Run a check from Report first, then this screen can check your apps.")
                current == null -> Note("Reading settings...")
                else -> {
                    val groups = WatcherRules.grouped(current)
                    groups[WatcherLevel.ATTENTION]?.let { findings ->
                        SectionLabel("Needs attention", color = Palette.SeverityHigh)
                        HairlineDivider()
                        findings.forEach { finding ->
                            AttentionRow(finding, onReview = { onRoute(appRoute(it)) }, onOpenSetting = { openSetting(finding) })
                            HairlineDivider()
                        }
                    }
                    groups[WatcherLevel.CHECK]?.let { findings ->
                        SectionLabel("Worth knowing", color = Palette.SeverityMedium)
                        HairlineDivider()
                        findings.forEach { finding ->
                            ListRow(
                                finding.title,
                                supporting = WatcherRows.checkLine(finding),
                                showChevron = true,
                                onClick = { openSetting(finding) },
                            )
                            HairlineDivider()
                        }
                    }
                    groups[WatcherLevel.FINE]?.let { findings ->
                        SectionLabel("All clear", color = Palette.Ok)
                        HairlineDivider()
                        findings.forEach { finding ->
                            AllClearRow(finding)
                            HairlineDivider()
                        }
                    }
                }
            }

            Text(
                "Some things cannot be checked from inside an app: other apps' usage access, screen overlays, " +
                    "all-files access, and which app is running a VPN.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextMuted,
                modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 24.dp, bottom = 32.dp),
            )
        }
    }
}

/** Advice for people who may be monitored by someone close to them, shown before any finding. */
@Composable
private fun SafetyNote() {
    BarredBlock(barColor = Palette.Ok, top = 4.dp, bottom = 4.dp) {
        Text(
            "Worried someone else set up your phone?",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Palette.Text,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Removing monitoring software can alert the person who installed it. Talk to a tech safety advocate " +
                "before you change anything.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
        )
        CopyableAddress(SAFETY_ADDRESS)
    }
}

/** A "needs attention" item: bar, title, the apps involved, what it means, and where to review it. */
@Composable
private fun AttentionRow(finding: WatcherFinding, onReview: (String) -> Unit, onOpenSetting: () -> Unit) {
    // Certificates are listed like apps, but there is no app to review.
    val reviewable = if (finding.kind == WatcherKind.CERTIFICATES) emptyList() else finding.apps
    BarredBlock(barColor = Palette.SeverityHigh, top = 20.dp, bottom = 20.dp) {
        Text(finding.title, style = MaterialTheme.typography.titleLarge, color = Palette.Text)
        WatcherRows.countLine(finding)?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = GarudaType.NumberSmall, color = Palette.TextMuted)
        }
        Spacer(Modifier.height(8.dp))
        Text(finding.explanation, style = MaterialTheme.typography.bodyMedium, color = Palette.TextDim)
        Spacer(Modifier.height(12.dp))
        when (reviewable.size) {
            0 -> PrimaryButton("Open Android setting", onClick = onOpenSetting)
            1 -> {
                PrimaryButton("Review app", onClick = { onReview(reviewable.single().packageName) })
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Open Android setting", onClick = onOpenSetting)
            }
            else -> {
                reviewable.forEach { app -> TextAction("Review ${app.appName}", onClick = { onReview(app.packageName) }) }
                Spacer(Modifier.height(6.dp))
                SecondaryButton("Open Android setting", onClick = onOpenSetting)
            }
        }
    }
}

/** A compact "all clear" row: name on the left, status on the right. */
@Composable
private fun AllClearRow(finding: WatcherFinding) {
    val row = WatcherRows.allClear(finding)
    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = ScreenGutter, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, color = Palette.Text, modifier = Modifier.weight(1f))
            Text(
                row.status,
                style = GarudaType.NumberMedium,
                color = if (row.isOn) Palette.Ok else Palette.TextMuted,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        if (finding.kind == WatcherKind.NOT_CHECKED) {
            Text(finding.explanation, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextMuted,
        modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 24.dp),
    )
}
