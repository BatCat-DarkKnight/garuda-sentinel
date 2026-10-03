package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.corbraytechnologies.garudasentinel.GarudaApp
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard

@Composable
fun AboutScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val version = remember { (context.applicationContext as GarudaApp).container.appVersion }
    GarudaScaffold(title = "About", onMenuClick = onMenuClick) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Garuda Sentinel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Version $version", style = MaterialTheme.typography.bodyMedium)
            Text(
                "A personal privacy tool that shows you the metadata your own phone holds, and explains what it could reveal.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SectionCard(title = "How it protects you") {
                Text("- No internet permission: the app cannot send data anywhere.", style = MaterialTheme.typography.bodySmall)
                Text("- Results stay in private app storage and are excluded from backups.", style = MaterialTheme.typography.bodySmall)
                Text("- Every permission is optional and explained.", style = MaterialTheme.typography.bodySmall)
                Text("- Exports happen only when you pick where to save them.", style = MaterialTheme.typography.bodySmall)
                Text("- One tap deletes everything.", style = MaterialTheme.typography.bodySmall)
            }
            Text("(c) 2026 Corbray Technologies", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
