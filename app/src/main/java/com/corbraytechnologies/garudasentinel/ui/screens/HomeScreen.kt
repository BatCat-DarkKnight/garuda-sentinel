package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.R
import com.corbraytechnologies.garudasentinel.permissions.MediaAccess
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.scan.ScanStep
import com.corbraytechnologies.garudasentinel.scan.StepStatus
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.NoticeCard
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.utils.ScanChanges
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

@Composable
fun HomeScreen(main: MainViewModel, onMenuClick: () -> Unit, onNavigate: (Dest) -> Unit) {
    val context = LocalContext.current
    val scan by main.scanState.collectAsStateWithLifecycle()
    val logs by main.scanLogs.collectAsStateWithLifecycle()
    val apps by main.apps.collectAsStateWithLifecycle()
    val media by main.media.collectAsStateWithLifecycle()
    val files by main.files.collectAsStateWithLifecycle()
    val changes by main.changes.collectAsStateWithLifecycle()

    var missing by remember { mutableStateOf(emptyList<String>()) }
    LifecycleResumeEffect(Unit) {
        missing = buildList {
            if (Permissions.mediaAccess(context) == MediaAccess.NONE) add("photos and videos")
            if (!Permissions.hasUsageAccess(context)) add("app usage")
        }
        onPauseOrDispose { }
    }

    val lastLog = logs.firstOrNull()

    GarudaScaffold(title = "", onMenuClick = onMenuClick) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "GARUDA\nSENTINEL",
                color = Palette.Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "See what your phone knows about you. Everything stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            AnimatedLogo()

            if (scan.running) {
                OutlinedButton(onClick = main::cancelScan, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Scanning... tap to cancel")
                }
            } else {
                Button(
                    onClick = main::startScan,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.OnAccent),
                ) {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("START SCAN", fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                }
            }

            if (scan.steps.isNotEmpty()) {
                SectionCard(title = if (scan.running) "Scanning" else "Last scan steps") {
                    ScanStep.entries.forEach { step -> StepRow(step, scan.steps[step] ?: StepStatus.Waiting) }
                }
            }

            if (missing.isNotEmpty()) {
                NoticeCard(
                    title = "Some data is hidden",
                    body = "Garuda Sentinel cannot see your ${missing.joinToString(" or ")} yet. Every permission is optional.",
                    actionLabel = "Review permissions",
                    onAction = { onNavigate(Dest.PERMISSIONS) },
                )
            }

            changes?.takeIf { !it.isEmpty }?.let { ChangesCard(it, onNavigate) }

            SectionCard(title = "On this device now") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CountTile("Apps", apps.size) { onNavigate(Dest.APPS) }
                    CountTile("Media", media.size) { onNavigate(Dest.MEDIA) }
                    CountTile("Files", files.size) { onNavigate(Dest.FILES) }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        lastLog?.let { "Last scan ${formatDate(it.finishedAt)} (took ${formatDuration(it.finishedAt - it.startedAt)})" }
                            ?: "No scan yet. Tap START SCAN to begin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(onClick = { onNavigate(Dest.YOUR_DATA) }, modifier = Modifier.fillMaxWidth()) {
                Text("What does this say about me?")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepRow(step: ScanStep, status: StepStatus) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        when (status) {
            StepStatus.Running -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            is StepStatus.Done -> Icon(Icons.Default.CheckCircle, null, tint = Palette.Ok, modifier = Modifier.size(16.dp))
            is StepStatus.Failed -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            else -> Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(step.label, style = MaterialTheme.typography.bodyMedium)
            val detail = when (status) {
                StepStatus.Waiting -> "Waiting"
                StepStatus.Running -> "Working..."
                is StepStatus.Done -> "${status.count} found"
                is StepStatus.Skipped -> "Skipped: ${status.reason}"
                is StepStatus.Failed -> "Failed: ${status.message}"
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "What changed since your last scan", shown only when something actually changed. */
@Composable
private fun ChangesCard(changes: ScanChanges, onNavigate: (Dest) -> Unit) {
    SectionCard(
        title = "What changed since your last scan",
        subtitle = "Compared with the scan on " + formatDate(changes.previousAt) + ".",
    ) {
        changes.newWatcherSignals.forEach { ChangeLine(it, attention = true) }
        changes.newPermissions.forEach { change ->
            ChangeLine("${change.app.appName} was allowed " + change.permissionLabels.joinToString().lowercase(), attention = true)
        }
        changes.newApps.forEach { ChangeLine("${it.appName} was installed") }
        changes.removedApps.forEach { ChangeLine("${it.appName} was removed") }
        if (changes.newWatcherSignals.isNotEmpty()) {
            TextButton(onClick = { onNavigate(Dest.WATCHERS) }) { Text("See who can watch") }
        }
    }
}

@Composable
private fun ChangeLine(text: String, attention: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Icon(
            if (attention) Icons.Default.PriorityHigh else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (attention) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CountTile(label: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Palette.Accent.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Palette.Accent)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AnimatedLogo() {
    val transition = rememberInfiniteTransition(label = "logo")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale",
    )
    val glow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow",
    )
    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(170.dp).background(Palette.Accent.copy(alpha = glow * 0.25f), RoundedCornerShape(85.dp)))
        Image(
            painter = painterResource(R.drawable.garuda_logo_white),
            contentDescription = "Garuda Sentinel logo",
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
            contentScale = ContentScale.Fit,
        )
    }
}
