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

import androidx.core.net.toUri
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.data.FileMetadataEntity
import com.corbraytechnologies.garudasentinel.ui.FilesViewModel
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.KeyValueRow
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.ui.containerViewModel
import com.corbraytechnologies.garudasentinel.utils.countOf
import com.corbraytechnologies.garudasentinel.utils.detailLine
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatFileSize
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

private const val LIST_LIMIT = 500

@Composable
fun FilesScreen(main: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = containerViewModel { FilesViewModel(it) }
    val tree by vm.treeUri.collectAsStateWithLifecycle()
    val files by main.files.collectAsStateWithLifecycle()
    val scan by main.scanState.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { vm.onFolderPicked(context, it) }
    }

    GarudaScaffold(title = "Files", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard(
                    title = "Scan a folder you choose",
                    subtitle = "Android does not let apps look through all your files, and Garuda Sentinel does not ask for that. " +
                        "Pick a folder (for example Download or Documents) and only that folder is read. " +
                        "Only names, sizes and dates are recorded, never file contents.",
                ) {
                    KeyValueRow("Folder", tree?.let { it.toUri().lastPathSegment?.substringAfter(':')?.ifBlank { "Storage root" } } ?: "None chosen")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { picker.launch(null) }, enabled = !scan.running) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (tree == null) "Choose folder" else "Change folder")
                        }
                        if (tree != null) {
                            OutlinedButton(onClick = { vm.forgetFolder(context) }, enabled = !scan.running) { Text("Forget folder") }
                        }
                    }
                }
            }
            if (files.isNotEmpty()) {
                item { FileSummary(files) }
                item {
                    Text(
                        if (files.size > LIST_LIMIT) "Showing newest $LIST_LIMIT of ${files.size}" else countOf(files.size, "file"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(files.take(LIST_LIMIT), key = { it.documentUri }) { FileRow(it) }
            } else if (tree != null && !scan.running) {
                item { Text("No files found yet. Run a check from Report, or pick another folder.", style = MaterialTheme.typography.bodyMedium) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FileSummary(files: List<FileMetadataEntity>) {
    val byType = remember(files) {
        files.groupingBy { it.extension.ifBlank { "(none)" } }.eachCount().entries.sortedByDescending { it.value }.take(6)
    }
    val largest = remember(files) { files.maxByOrNull { it.fileSize } }
    SectionCard(title = "What this folder reveals") {
        KeyValueRow("Files", files.size.toString())
        KeyValueRow("Total size", formatFileSize(files.sumOf { it.fileSize }))
        KeyValueRow("Largest", largest?.let { "${it.fileName} (${formatFileSize(it.fileSize)})" })
        KeyValueRow("Newest", files.maxByOrNull { it.lastModified }?.let { formatDate(it.lastModified) })
        KeyValueRow("Oldest", files.minByOrNull { it.lastModified }?.let { formatDate(it.lastModified) })
        Spacer(Modifier.height(4.dp))
        Text("Most common types", style = MaterialTheme.typography.titleSmall)
        byType.forEach { (ext, count) -> KeyValueRow(".$ext", count.toString()) }
    }
}

@Composable
private fun FileRow(file: FileMetadataEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = Palette.Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    detailLine(file.displayPath.substringBeforeLast('/', ""), formatFileSize(file.fileSize), formatDate(file.lastModified)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
