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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.ui.HistoryViewModel
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ScreenGutter
import com.corbraytechnologies.garudasentinel.ui.containerViewModel
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.utils.countOf
import com.corbraytechnologies.garudasentinel.utils.detailLine
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration

/** Every past check with its counts and anything that was skipped. Opened from Controls. */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm = containerViewModel { HistoryViewModel(it) }
    val logs by vm.logs.collectAsStateWithLifecycle()

    GarudaScaffold(title = "Past checks", onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (logs.isEmpty()) {
                item {
                    Text(
                        "No checks yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextMuted,
                        modifier = Modifier.padding(ScreenGutter),
                    )
                }
            }
            items(logs, key = { it.id }) { log ->
                ScanLogRow(log, onDelete = { vm.deleteLog(log.id) })
                HairlineDivider()
            }
        }
    }
}

@Composable
private fun ScanLogRow(log: ScanLogEntity, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = ScreenGutter, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(formatDate(log.startedAt), style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
            Text(
                detailLine(countOf(log.appsCount, "app"), "${log.mediaCount} media", countOf(log.filesCount, "file"), countOf(log.usageCount, "app") + " with usage"),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
            Text(
                "Took ${formatDuration(log.finishedAt - log.startedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextMuted,
            )
            log.notes.lines().filter { it.isNotBlank() }.forEach {
                Text("- $it", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete this entry", tint = Palette.TextMuted)
        }
    }
}
