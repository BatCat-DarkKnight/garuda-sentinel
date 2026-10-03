package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.export.EncryptedExport
import com.corbraytechnologies.garudasentinel.export.ExportCategory
import com.corbraytechnologies.garudasentinel.export.ExportFormat
import com.corbraytechnologies.garudasentinel.ui.ControlsViewModel
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.components.DestructiveButton
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ListRow
import com.corbraytechnologies.garudasentinel.ui.components.PrimaryButton
import com.corbraytechnologies.garudasentinel.ui.components.ScreenGutter
import com.corbraytechnologies.garudasentinel.ui.components.ScreenTitle
import com.corbraytechnologies.garudasentinel.ui.components.SectionLabel
import com.corbraytechnologies.garudasentinel.ui.components.TextAction
import com.corbraytechnologies.garudasentinel.ui.components.restartApp
import com.corbraytechnologies.garudasentinel.ui.containerViewModel
import com.corbraytechnologies.garudasentinel.ui.theme.GarudaType
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.ui.theme.SmallCorner
import com.corbraytechnologies.garudasentinel.utils.DeleteSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything settings-like: export, how results are kept, screen privacy, past checks and Delete all. */
@Composable
fun ControlsScreen(onNavigate: (Dest) -> Unit) {
    val vm = containerViewModel { ControlsViewModel(it) }
    val selection by vm.selection.collectAsStateWithLifecycle()
    val includeGps by vm.includePhotoLocations.collectAsStateWithLifecycle()
    val format by vm.format.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val blockScreenshots by vm.blockScreenshots.collectAsStateWithLifecycle()
    val savedChecks by vm.savedChecks.collectAsStateWithLifecycle()
    val locationReadings by vm.locationReadings.collectAsStateWithLifecycle()
    val tree by vm.treeUri.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmStorageMode by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Passwords stay in plain remember (not saved state), so they never reach the saved instance bundle.
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val zip = format == ExportFormat.ENCRYPTED_ZIP
    val passwordProblem = if (zip) EncryptedExport.passwordProblem(password, repeat) else null

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

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        ScreenTitle("Controls")

        SectionLabel("Export a copy")
        Text(
            "One file holding everything you tick. Together it says more about you than any single part, so protect it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
            modifier = Modifier.padding(horizontal = ScreenGutter),
        )
        Spacer(Modifier.height(12.dp))
        HairlineDivider()
        ExportCategory.entries.forEach { category ->
            CheckRow(category.label, checked = category in selection, onToggle = { vm.toggle(category) })
            HairlineDivider(inset = true)
            if (category == ExportCategory.MEDIA) {
                CheckRow(
                    "Photo locations",
                    supporting = "Off by default. These can show where you live and work.",
                    supportingColor = Palette.SeverityMedium,
                    checked = includeGps && ExportCategory.MEDIA in selection,
                    enabled = ExportCategory.MEDIA in selection,
                    onToggle = { vm.includePhotoLocations.value = !includeGps },
                )
                HairlineDivider(inset = true)
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.selectableGroup()) {
            RadioRow(
                "Password-protected ZIP",
                supporting = "Recommended. Opens with 7-Zip on Windows, Keka on Mac.",
                selected = zip,
                onSelect = { vm.format.value = ExportFormat.ENCRYPTED_ZIP },
            )
            RadioRow("Plain JSON file", selected = !zip, onSelect = { vm.format.value = ExportFormat.PLAIN_JSON })
        }
        if (zip) {
            Column(Modifier.padding(horizontal = ScreenGutter)) {
                Spacer(Modifier.height(12.dp))
                PasswordField("Password", password) { password = it }
                EncryptedExport.passwordHint(password)?.let { hint ->
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = Palette.SeverityMedium, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                PasswordField("Repeat password", repeat) { repeat = it }
                Text(
                    passwordProblem ?: "If you forget the password, the file cannot be opened.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (passwordProblem != null) Palette.SeverityHigh else Palette.TextMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            "Choose where to save",
            onClick = { if (zip) saveZip.launch(defaultFileName("zip")) else saveFile.launch(defaultFileName("json")) },
            enabled = !busy && selection.isNotEmpty() && passwordProblem == null,
            modifier = Modifier.padding(horizontal = ScreenGutter),
        )
        message?.let { msg ->
            Column(Modifier.padding(horizontal = ScreenGutter, vertical = 8.dp)) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
                TextAction("Dismiss", onClick = vm::clearMessage)
            }
        }

        SectionLabel("Keeping results")
        HairlineDivider()
        SwitchRow(
            "Forget results when I close the app",
            supporting = "Nothing is written to storage. Turns off \"since your last check\".",
            checked = vm.memoryOnly,
            enabled = !busy,
            onToggle = { confirmStorageMode = true },
        )
        HairlineDivider(inset = true)
        SwitchRow(
            "Block screenshots of this app",
            supporting = "Also stops you taking your own screenshots and screen recordings.",
            checked = blockScreenshots,
            onToggle = { vm.setBlockScreenshots(!blockScreenshots) },
        )
        HairlineDivider(inset = true)
        ListRow("Past checks", value = savedChecks.toString(), showChevron = true, onClick = { onNavigate(Dest.HISTORY) })
        HairlineDivider()

        SectionLabel("Delete", color = Palette.SeverityHigh)
        Text(
            DeleteSummary.sentence(savedChecks, locationReadings, tree?.let { it.toUri().lastPathSegment?.substringAfter(':') ?: "" }),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
            modifier = Modifier.padding(horizontal = ScreenGutter),
        )
        Spacer(Modifier.height(16.dp))
        DestructiveButton(
            "Delete all scan data",
            onClick = { confirmDelete = true },
            enabled = !busy,
            modifier = Modifier.padding(horizontal = ScreenGutter),
        )
        Spacer(Modifier.height(32.dp))
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

/** A 50dp row with a checkbox; the whole row toggles it. */
@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    supporting: String? = null,
    supportingColor: Color = Palette.TextDim,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = ScreenGutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) Palette.Text else Palette.TextMuted)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = supportingColor) }
        }
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** A radio option with an optional second line; the whole row selects it. */
@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit, supporting: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = ScreenGutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim) }
        }
    }
}

/** A switch row with a second line; the whole row toggles it. */
@Composable
private fun SwitchRow(label: String, supporting: String, checked: Boolean, onToggle: () -> Unit, enabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = ScreenGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.Text)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Mono label above a 3dp-cornered field with the strong outline. */
@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    Text(label.uppercase(), style = GarudaType.SectionLabel, color = Palette.TextMuted)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        shape = SmallCorner,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Palette.OutlineStrong,
            focusedBorderColor = Palette.Accent,
        ),
        // The visible label is a separate Mono text, so TalkBack gets it here.
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
    )
}

/** The outer file name, which the user can change in the Save dialog. The ZIP's inner file name is always generic. */
private fun defaultFileName(extension: String): String =
    "garuda-sentinel-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + "." + extension
