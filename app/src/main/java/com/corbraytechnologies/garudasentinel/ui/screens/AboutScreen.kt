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
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember { (context.applicationContext as GarudaApp).container.appVersion }
    GarudaScaffold(title = "About", onBack = onBack) { padding ->
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
