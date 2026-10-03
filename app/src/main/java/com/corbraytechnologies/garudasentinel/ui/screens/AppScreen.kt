package com.corbraytechnologies.garudasentinel.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.EmptyStateMessage
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.KeyValueRow
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.utils.AppLists
import com.corbraytechnologies.garudasentinel.utils.SensitivePermissions
import com.corbraytechnologies.garudasentinel.utils.countOf
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

private enum class AppFilter(val label: String) { USER("Installed by you"), SYSTEM("System"), ALL("All") }
/** Rows above the first app: summary, search, filter chips, sort chips and the count. */
private const val HEADER_ITEMS = 5

private enum class AppSort(val label: String) { NAME("Name"), UPDATED("Recently updated"), INSTALLED("Recently installed"), SENSITIVE("Most access") }

@Composable
fun AppScreen(main: MainViewModel, onBack: () -> Unit, focusPackage: String? = null, sortByAccess: Boolean = false) {
    val apps by main.apps.collectAsStateWithLifecycle()
    val ownPackage = LocalContext.current.packageName
    // An app opened from a finding is shown in the list that holds it, with its row open.
    var filter by rememberSaveable {
        mutableStateOf(if (apps.any { it.packageName == focusPackage && it.isSystemApp }) AppFilter.ALL else AppFilter.USER)
    }
    var sort by rememberSaveable { mutableStateOf(if (sortByAccess) AppSort.SENSITIVE else AppSort.NAME) }
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    var focused by rememberSaveable { mutableStateOf(false) }
    val shown = remember(apps, filter, sort, query) {
        when (filter) {
            AppFilter.USER -> AppLists.userInstalled(apps, ownPackage)
            AppFilter.SYSTEM -> apps.filter { it.isSystemApp }
            AppFilter.ALL -> apps
        }.asSequence()
            .filter { query.isBlank() || it.appName.contains(query, true) || it.packageName.contains(query, true) }
            .let { seq ->
                when (sort) {
                    AppSort.NAME -> seq.sortedBy { it.appName.lowercase() }
                    AppSort.UPDATED -> seq.sortedByDescending { it.lastUpdateTime }
                    AppSort.INSTALLED -> seq.sortedByDescending { it.firstInstallTime }
                    AppSort.SENSITIVE -> seq.sortedByDescending { SensitivePermissions.sensitiveOf(it.grantedPermissionList).size }
                }
            }
            .toList()
    }

    LaunchedEffect(focusPackage, shown) {
        if (focusPackage == null || focused) return@LaunchedEffect
        val index = shown.indexOfFirst { it.packageName == focusPackage }
        if (index >= 0) {
            listState.scrollToItem(HEADER_ITEMS + index)
            focused = true
        }
    }

    GarudaScaffold(title = "Installed Apps", onBack = onBack) { padding ->
        if (apps.isEmpty()) {
            EmptyStateMessage(Icons.Default.Apps, "No apps yet. Run a check from Report.", Modifier.padding(padding))
            return@GarudaScaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { AppSummary(apps) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilter.entries.forEach { FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it.label) }) }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSort.entries.forEach { FilterChip(selected = sort == it, onClick = { sort = it }, label = { Text(it.label) }) }
                }
            }
            item { Text(countOf(shown.size, "app"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(shown, key = { it.packageName }) { AppCard(it, startExpanded = it.packageName == focusPackage) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AppSummary(apps: List<AppMetadataEntity>) {
    val ownPackage = LocalContext.current.packageName
    val user = AppLists.userInstalled(apps, ownPackage)
    val withLocation = user.count { app -> app.grantedPermissionList.any { it == "android.permission.ACCESS_FINE_LOCATION" || it == "android.permission.ACCESS_COARSE_LOCATION" } }
    val withMic = user.count { "android.permission.RECORD_AUDIO" in it.grantedPermissionList }
    val withCamera = user.count { "android.permission.CAMERA" in it.grantedPermissionList }
    SectionCard(
        title = "What your apps can access",
        subtitle = "Counts only apps you installed, not counting Garuda Sentinel (it is listed under All). \"Allowed\" means the permission is granted right now.",
    ) {
        KeyValueRow("Apps you installed", user.size.toString())
        KeyValueRow("System apps", apps.count { it.isSystemApp }.toString())
        KeyValueRow("Allowed location", withLocation.toString())
        KeyValueRow("Allowed microphone", withMic.toString())
        KeyValueRow("Allowed camera", withCamera.toString())
    }
}

@Composable
private fun AppCard(app: AppMetadataEntity, startExpanded: Boolean = false) {
    var expanded by rememberSaveable(app.packageName) { mutableStateOf(startExpanded) }
    val context = LocalContext.current
    val permissions = remember(app) {
        // Special access can only be checked for this app itself, through AppOpsManager.
        val known = if (app.packageName == context.packageName) {
            mapOf("android.permission.PACKAGE_USAGE_STATS" to Permissions.hasUsageAccess(context))
        } else {
            emptyMap()
        }
        SensitivePermissions.breakdown(app.permissionList, app.grantedPermissionList, known)
    }
    val granted = permissions.allowed
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${app.appCategory} (estimated)  |  v${app.versionName.ifBlank { "?" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (granted.isNotEmpty()) {
                        Text(
                            "Allowed: " + granted.joinToString { it.label },
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Accent,
                        )
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                KeyValueRow("Package", app.packageName)
                KeyValueRow("Installed", formatDate(app.firstInstallTime))
                KeyValueRow("Last updated", formatDate(app.lastUpdateTime))
                KeyValueRow("Installed by", installerLabel(app.installer))
                KeyValueRow("Targets Android API", app.targetSdkVersion.toString())
                KeyValueRow("Permissions asked", app.permissionList.size.toString())
                if (granted.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Allowed now", style = MaterialTheme.typography.titleSmall, color = Palette.Accent)
                    granted.forEach { Text("${it.label}: ${it.why}", style = MaterialTheme.typography.bodySmall) }
                }
                if (permissions.notAllowed.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Asks for, but not allowed", style = MaterialTheme.typography.titleSmall)
                    Text(permissions.notAllowed.joinToString { it.label }, style = MaterialTheme.typography.bodySmall)
                }
                if (permissions.unknown.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Special access, can't be checked", style = MaterialTheme.typography.titleSmall)
                    Text(
                        permissions.unknown.joinToString { it.label } +
                            ". Turned on in Settings, and Android does not let other apps see whether it is on.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun installerLabel(installer: String?): String = when (installer) {
    null -> "Unknown or preinstalled"
    "com.android.vending" -> "Google Play Store"
    "com.sec.android.app.samsungapps" -> "Galaxy Store"
    "com.amazon.venezia" -> "Amazon Appstore"
    "com.google.android.packageinstaller", "com.android.packageinstaller" -> "Installed from a file (sideloaded)"
    else -> installer
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(44.dp))
    } else {
        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(44.dp))
    }
}
