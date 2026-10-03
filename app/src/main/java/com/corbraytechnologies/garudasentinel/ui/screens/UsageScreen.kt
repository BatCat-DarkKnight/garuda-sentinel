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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.model.AppUsageInfo
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.KeyValueRow
import com.corbraytechnologies.garudasentinel.ui.components.NoticeCard
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

private enum class UsageRange(val label: String) { TODAY("Today"), WEEK("Last 7 days") }

@Composable
fun UsageScreen(main: MainViewModel, onBack: () -> Unit, onNavigate: (Dest) -> Unit) {
    val context = LocalContext.current
    val usage by main.usage.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(true) }
    var range by rememberSaveable { mutableStateOf(UsageRange.WEEK) }
    var browsersOnly by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        granted = Permissions.hasUsageAccess(context)
        if (granted) main.refreshUsage()
        onPauseOrDispose { }
    }

    GarudaScaffold(
        title = "App Usage",
        onBack = onBack,
        actions = {
            IconButton(onClick = { main.refreshUsage() }, enabled = granted) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!granted) {
                item {
                    NoticeCard(
                        title = "Usage access needed",
                        body = "Android keeps a record of which apps you open and for how long. Allow Usage access to see it. " +
                            "This is a special setting you turn on in system Settings.",
                        actionLabel = "Permissions",
                        onAction = { onNavigate(Dest.PERMISSIONS) },
                    )
                }
                return@LazyColumn
            }
            val summary = usage
            if (summary == null) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                return@LazyColumn
            }
            val apps = summary.apps.filter { !browsersOnly || it.isBrowser }
                .sortedByDescending { if (range == UsageRange.TODAY) it.foregroundTodayMs else it.foregroundLast7DaysMs }
                .filter { if (range == UsageRange.TODAY) it.foregroundTodayMs > 0 || it.opensToday > 0 else true }
            val browsers = summary.apps.filter { it.isBrowser }

            item {
                SectionCard(
                    title = "What your usage history shows",
                    subtitle = "Read from Android's own usage records on ${formatDate(summary.collectedAt)}.",
                ) {
                    KeyValueRow("Screen on today", formatDuration(summary.screenOnTodayMs))
                    KeyValueRow("App opens today", summary.apps.sumOf { it.opensToday }.toString())
                    KeyValueRow("Apps used this week", summary.apps.count { it.foregroundLast7DaysMs > 0 }.toString())
                    KeyValueRow("Most used this week", summary.apps.firstOrNull()?.let { "${it.appName} (${formatDuration(it.foregroundLast7DaysMs)})" })
                    KeyValueRow(
                        "Browsing time this week",
                        if (browsers.isEmpty()) "No browser use recorded" else formatDuration(browsers.sumOf { it.foregroundLast7DaysMs }),
                    )
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UsageRange.entries.forEach { FilterChip(selected = range == it, onClick = { range = it }, label = { Text(it.label) }) }
                    FilterChip(selected = browsersOnly, onClick = { browsersOnly = !browsersOnly }, label = { Text("Browsers only") })
                }
            }
            val max = apps.maxOfOrNull { if (range == UsageRange.TODAY) it.foregroundTodayMs else it.foregroundLast7DaysMs } ?: 0L
            items(apps, key = { it.packageName }) { UsageRow(it, range, max) }
            item {
                Text(
                    "Garuda Sentinel records how long each app was open, not what you did inside it. Browser rows show time only, never the sites you visited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun UsageRow(app: AppUsageInfo, range: UsageRange, max: Long) {
    val time = if (range == UsageRange.TODAY) app.foregroundTodayMs else app.foregroundLast7DaysMs
    val opens = if (range == UsageRange.TODAY) app.opensToday else app.opensLast7Days
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                app.appName + if (app.isBrowser) "  (browser)" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formatDuration(time)}  |  ${openedLabel(opens)}" +
                    (app.lastUsedAt?.let { "  |  last used ${formatDate(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (max > 0) time.toFloat() / max else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = Palette.Accent,
            )
        }
    }
}

private fun openedLabel(opens: Int) = when (opens) {
    0 -> "not opened"
    1 -> "opened once"
    else -> "opened $opens times"
}
