package com.corbraytechnologies.garudasentinel.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.corbraytechnologies.garudasentinel.permissions.MediaAccess
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.ui.components.openSettings
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

private enum class Grant { YES, PARTIAL, NO }

@Composable
fun PermissionsScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    // Bumped on resume and after each request so the statuses below are re-read.
    var refreshKey by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refreshKey++
        onPauseOrDispose { }
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshKey++ }

    val media = remember(refreshKey) { Permissions.mediaAccess(context) }
    val audio = remember(refreshKey) { Permissions.hasAudioAccess(context) }
    val photoLocation = remember(refreshKey) { Permissions.hasMediaLocation(context) }
    val usage = remember(refreshKey) { Permissions.hasUsageAccess(context) }
    val location = remember(refreshKey) {
        when {
            Permissions.hasPreciseLocation(context) -> Grant.YES
            Permissions.hasAnyLocation(context) -> Grant.PARTIAL
            else -> Grant.NO
        }
    }

    GarudaScaffold(title = "Permissions", onMenuClick = onMenuClick) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard(
                    title = "Your choice, every time",
                    subtitle = "Every permission is optional. Without one, that part of the scan is skipped and the app tells you. " +
                        "Garuda Sentinel does not have internet access, so nothing it reads can be sent anywhere.",
                ) {}
            }
            item {
                PermissionRow(
                    title = "Photos and videos",
                    why = "Lists your photos and videos with dates, sizes, cameras and folders.",
                    grant = when (media) {
                        MediaAccess.FULL -> Grant.YES
                        MediaAccess.PARTIAL -> Grant.PARTIAL
                        MediaAccess.NONE -> Grant.NO
                    },
                    partialNote = "Only photos you selected",
                    actionLabel = "Allow",
                    onAction = { request.launch(Permissions.mediaPermissions()) },
                )
            }
            item {
                PermissionRow(
                    title = "Music and audio",
                    why = "Lists audio files with artist, album and length.",
                    grant = if (audio) Grant.YES else Grant.NO,
                    actionLabel = "Allow",
                    onAction = { request.launch(Permissions.mediaPermissions()) },
                )
            }
            item {
                // Without full photo access, Android silently refuses a request for photo location
                // on its own, but grants it when it is asked for together with the photo permissions.
                val withMedia = media != MediaAccess.FULL
                PermissionRow(
                    title = "Photo locations",
                    why = "Lets the app read the GPS coordinates saved inside your photos. Android hides them otherwise.",
                    grant = if (photoLocation) Grant.YES else Grant.NO,
                    actionLabel = "Allow",
                    onAction = {
                        request.launch(
                            if (withMedia) Permissions.mediaPermissions() + Permissions.MEDIA_LOCATION
                            else arrayOf(Permissions.MEDIA_LOCATION),
                        )
                    },
                )
            }
            item {
                PermissionRow(
                    title = "Usage access",
                    why = "Shows which apps you use, how often and for how long. Turned on in system Settings, not with a pop-up.",
                    grant = if (usage) Grant.YES else Grant.NO,
                    actionLabel = "Open Settings",
                    onAction = { context.openSettings(Permissions.usageAccessSettingsIntent(context)) },
                )
            }
            item {
                PermissionRow(
                    title = "Location",
                    why = "Used only when you tap \"Read my location\" on the Device screen, to show what apps can learn about where you are.",
                    grant = location,
                    partialNote = "Approximate only",
                    actionLabel = "Allow",
                    onAction = { request.launch(Permissions.locationPermissions) },
                )
            }
            item {
                PermissionRow(
                    title = "Installed apps and network state",
                    why = "Granted automatically when the app is installed. Used to list your apps and your current connection.",
                    grant = Grant.YES,
                )
            }
            item {
                SectionCard(title = "If Android says \"Restricted setting\"") {
                    Text(
                        "On Android 13 and later, if you installed Garuda Sentinel from a file, turning on Usage access can show a " +
                            "\"Restricted setting\" message instead of the switch. Tap OK, then open App info, tap the three-dot menu " +
                            "(top right), choose \"Allow restricted settings\" and confirm with your PIN or fingerprint. Then turn on " +
                            "Usage access again. If you said \"Don't allow\" to a pop-up twice, Android stops asking; change it in " +
                            "App info > Permissions instead.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { context.openSettings(Permissions.appDetailsSettingsIntent(context)) }) {
                        Text("Open App info")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    why: String,
    grant: Grant,
    partialNote: String = "Partly allowed",
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (grant) {
                    Grant.YES -> Icons.Default.CheckCircle
                    Grant.PARTIAL -> Icons.Default.RemoveCircle
                    Grant.NO -> Icons.Default.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = when (grant) {
                    Grant.YES -> Palette.Ok
                    Grant.PARTIAL -> MaterialTheme.colorScheme.primary
                    Grant.NO -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    when (grant) {
                        Grant.YES -> "Allowed"
                        Grant.PARTIAL -> partialNote
                        Grant.NO -> "Not allowed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (grant != Grant.YES && actionLabel != null) {
                OutlinedButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
