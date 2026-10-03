package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.export.EncryptedExport
import com.corbraytechnologies.garudasentinel.export.ExportCategory
import com.corbraytechnologies.garudasentinel.ui.HistoryViewModel
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.ui.components.restartApp
import com.corbraytechnologies.garudasentinel.ui.containerViewModel
import com.corbraytechnologies.garudasentinel.utils.countOf
import com.corbraytechnologies.garudasentinel.utils.detailLine
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

@Composable
fun HistoryScreen(onMenuClick: () -> Unit) {
    val vm = containerViewModel { HistoryViewModel(it) }
    val logs by vm.logs.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val includeGps by vm.includePhotoLocations.collectAsStateWithLifecycle()
    val blockScreenshots by vm.blockScreenshots.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmStorageMode by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // Passwords stay in plain remember (not saved state), so they never reach the saved instance bundle.
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val passwordProblem = if (encrypt) EncryptedExport.passwordProblem(password, repeat) else null

    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(it) }
    }
    val saveZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            vm.export(uri, password.toCharArray())
            password = ""
            repeat = ""
        }
    }

    GarudaScaffold(title = "Scan History & Export", onMenuClick = onMenuClick) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard(
                    title = "Export your data",
                    subtitle = "Creates one file in a place you choose. Nothing is sent anywhere. " +
                        "Without a password the file is not encrypted, so keep it somewhere safe.",
                ) {
                    ExportCategory.entries.forEach { category ->
                        CheckRow(label = category.label, checked = category in selection, onToggle = { vm.toggle(category) })
                    }
                    CheckRow(
                        label = "Include photo locations (GPS)",
                        checked = includeGps && ExportCategory.MEDIA in selection,
                        enabled = ExportCategory.MEDIA in selection,
                        onToggle = { vm.includePhotoLocations.value = !includeGps },
                    )
                    Text(
                        "Photo locations can reveal where you live, work and sleep.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CheckRow(label = "Protect with a password (encrypted ZIP)", checked = encrypt, onToggle = { encrypt = !encrypt })
                    if (encrypt) {
                        PasswordField("Password (at least ${EncryptedExport.MIN_PASSWORD_LENGTH} characters)", password) { password = it }
                        EncryptedExport.passwordHint(password)?.let { hint ->
                            Text(hint, style = MaterialTheme.typography.bodySmall, color = Palette.SeverityMedium)
                        }
                        PasswordField("Repeat password", repeat) { repeat = it }
                        Text(
                            passwordProblem ?: ("Opens with 7-Zip on Windows, or Keka or The Unarchiver on a Mac. " +
                                "If you forget the password, the file cannot be opened."),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (passwordProblem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { if (encrypt) saveZip.launch(defaultFileName("zip")) else saveFile.launch(defaultFileName("json")) },
                        enabled = !busy && selection.isNotEmpty() && passwordProblem == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export to a file...") }
                    message?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = vm::clearMessage) { Text("Dismiss") }
                    }
                }
            }
            item {
                SectionCard(
                    title = "Screen privacy",
                    subtitle = "The app is already left out of the recent-apps preview on Android 13 and later, so " +
                        "results do not show there.",
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Block screenshots of this app", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Also stops you taking your own screenshots and screen recordings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = blockScreenshots, onCheckedChange = { vm.setBlockScreenshots(it) })
                    }
                }
            }
            item {
                SectionCard(
                    title = "Forget results when I close the app",
                    subtitle = if (vm.memoryOnly) {
                        "On. Results are kept in memory only and disappear when the app is closed or the phone restarts."
                    } else {
                        "Off. Results are saved in the app's private storage until you delete them."
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Forget results when I close the app", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = vm.memoryOnly, onCheckedChange = { confirmStorageMode = true }, enabled = !busy)
                    }
                }
            }
            item {
                SectionCard(
                    title = "Delete everything",
                    subtitle = "Removes all scan results, saved locations and folder access from this app. Exported files you saved are not touched.",
                ) {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !busy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete all scan data") }
                }
            }
            item {
                Text("Past scans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (logs.isEmpty()) {
                item { Text("No scans yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(logs, key = { it.id }) { log -> ScanLogCard(log, onDelete = { vm.deleteLog(log.id) }) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (confirmStorageMode) {
        val turningOn = !vm.memoryOnly
        AlertDialog(
            onDismissRequest = { confirmStorageMode = false },
            title = { Text(if (turningOn) "Stop saving scan results?" else "Save scan results again?") },
            text = {
                Text(
                    if (turningOn) {
                        "Saved results are deleted now and the app restarts. After that, results are kept in memory only."
                    } else {
                        "Results in memory are cleared and the app restarts. After that, scans are saved in the app's private storage."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStorageMode = false
                    vm.setMemoryOnly(turningOn) { context.restartApp() }
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { confirmStorageMode = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all scan data?") },
            text = { Text("This cannot be undone. You can scan again at any time.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAll() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ScanLogCard(log: ScanLogEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(formatDate(log.startedAt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    detailLine(countOf(log.appsCount, "app"), "${log.mediaCount} media", countOf(log.filesCount, "file"), countOf(log.usageCount, "app") + " with usage"),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Took ${formatDuration(log.finishedAt - log.startedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                log.notes.lines().filter { it.isNotBlank() }.forEach {
                    Text("- $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete this entry") }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit, enabled: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The outer file name, which the user can change in the Save dialog. The ZIP's inner file name is always generic. */
private fun defaultFileName(extension: String): String =
    "garuda-sentinel-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + "." + extension
