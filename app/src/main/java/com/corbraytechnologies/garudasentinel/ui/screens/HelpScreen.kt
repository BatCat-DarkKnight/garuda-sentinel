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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ListRow
import com.corbraytechnologies.garudasentinel.ui.components.ScreenTitle

/** The Help tab: permissions, questions and answers, and version details. */
@Composable
fun HelpScreen(onNavigate: (Dest) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Help")
        Spacer(Modifier.height(16.dp))
        HairlineDivider()
        ListRow("Permissions", supporting = "What each one unlocks, and how to allow it", showChevron = true, onClick = { onNavigate(Dest.PERMISSIONS) })
        HairlineDivider(inset = true)
        ListRow("FAQ", supporting = "Common questions", showChevron = true, onClick = { onNavigate(Dest.FAQ) })
        HairlineDivider(inset = true)
        ListRow("About", supporting = "Version and how the app protects you", showChevron = true, onClick = { onNavigate(Dest.ABOUT) })
        HairlineDivider()
        Spacer(Modifier.height(24.dp))
    }
}
