package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.model.WatcherFinding
import com.corbraytechnologies.garudasentinel.model.WatcherLevel
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.WatchersViewModel
import com.corbraytechnologies.garudasentinel.ui.components.ConfirmOpenLink
import com.corbraytechnologies.garudasentinel.ui.components.EmptyStateMessage
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.ui.components.openSettings
import com.corbraytechnologies.garudasentinel.ui.containerViewModel
import com.corbraytechnologies.garudasentinel.utils.WatcherRules
import com.corbraytechnologies.garudasentinel.utils.countOf

private const val SAFETY_URL = "https://stopstalkerware.org"

/**
 * Shows which apps and settings on this phone can watch the user. Everything here is read only;
 * removing anything is left to the system screens.
 */
@Composable
fun WatchersScreen(main: MainViewModel, onBack: () -> Unit, onNavigate: (Dest) -> Unit) {
    val vm = containerViewModel { WatchersViewModel(it) }
    val apps by main.apps.collectAsStateWithLifecycle()
    val signals by vm.signals.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var linkToOpen by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) vm.refresh()
    }

    GarudaScaffold(
        title = "Who can watch",
        onBack = onBack,
        actions = {
            IconButton(onClick = { vm.refresh() }, enabled = !busy && apps.isNotEmpty()) {
                Icon(Icons.Default.Visibility, contentDescription = "Check again")
            }
        },
    ) { padding ->
        val current = signals
        if (apps.isEmpty()) {
            EmptyStateMessage(Icons.Default.Visibility, "Run a check from Report first, then this screen can check your apps.", Modifier.padding(padding))
            return@GarudaScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard(
                    title = "Before you change anything",
                    subtitle = "If you think someone else may have set up monitoring on your phone, removing it can warn them " +
                        "that you know. If you are worried about your safety, talk to a domestic violence or tech safety " +
                        "advocate first, ideally from a device the other person cannot see.",
                ) {
                    TextButton(onClick = { linkToOpen = SAFETY_URL }) { Text("Open stopstalkerware.org") }
                }
            }
            if (current == null) {
                item { Text("Checking...", style = MaterialTheme.typography.bodyMedium) }
            } else {
                val groups = WatcherRules.grouped(current)
                item {
                    val attention = WatcherRules.attentionCount(current)
                    SectionCard(
                        title = if (attention == 0) "Nothing needs your attention" else "${countOf(attention, "thing")} to look at",
                        subtitle = "Read from this phone with ordinary app permissions. Garuda Sentinel only reads these " +
                            "settings; it never changes them.",
                    ) {}
                }
                groups.forEach { (level, findings) ->
                    item { Text(levelTitle(level), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(findings.size) { index ->
                        FindingCard(findings[index]) { packageName ->
                            context.openSettings(vm.appSettingsIntent(packageName))
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { onNavigate(Dest.APPS) }, modifier = Modifier.fillMaxWidth()) {
                        Text("See all apps and their permissions")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    ConfirmOpenLink(url = linkToOpen, onDismiss = { linkToOpen = null })
}

private fun levelTitle(level: WatcherLevel): String = when (level) {
    WatcherLevel.ATTENTION -> "Needs attention"
    WatcherLevel.CHECK -> "Worth knowing"
    WatcherLevel.FINE -> "All clear"
}

@Composable
private fun FindingCard(finding: WatcherFinding, onOpenApp: (String) -> Unit) {
    SectionCard(title = finding.title, subtitle = finding.explanation) {
        finding.apps.forEach { app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onOpenApp(app.packageName) },
            ) {
                Column(Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(app.detail, app.packageName.takeIf { it != app.appName }).joinToString("  |  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("Open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
