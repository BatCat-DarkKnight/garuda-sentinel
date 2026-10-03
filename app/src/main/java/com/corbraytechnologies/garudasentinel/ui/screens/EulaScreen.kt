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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// DRAFT license and privacy text. Have it reviewed before public release.
private val EULA_TEXT = """
Effective date: 2026-10-03

This End User License Agreement is between you and Corbray Technologies and covers your use of the Garuda Sentinel Android app ("the App").

1. What the App does
The App reads information that is already on your phone, such as installed apps, photo and video details, app usage records, and device settings, and shows it to you so you can understand it.

2. Your data stays on your phone
The App does not have permission to use the internet. It does not send your data to Corbray Technologies or anyone else. Scan results are stored only in the App's private storage on this phone and are excluded from backups.

3. Exports are your choice
If you export your data, the App writes one file to a location you pick. You can protect that file with a password (an encrypted ZIP file) or save it as a plain file that is not encrypted. What you do with it is up to you.

4. Deleting your data
You can delete all scan results at any time from Controls. Uninstalling the App also deletes them.

5. License
Corbray Technologies grants you a personal, non-transferable, non-exclusive license to use the App. You may not resell the App.

6. No warranty
The App is provided "as is", without warranties of any kind. Some information depends on your phone maker and Android version and may be incomplete.

7. Limitation of liability
To the extent allowed by law, Corbray Technologies is not liable for damages arising from use of the App.

Contact: info@corbraytechnologies.com
(c) 2026 Corbray Technologies
""".trimIndent()

@Composable
fun EulaScreen(onAccept: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Before you start", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Garuda Sentinel shows you what your phone knows about you. Nothing leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(EULA_TEXT, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text("I agree") }
        }
    }
}
