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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.MainViewModel
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ListRow
import com.corbraytechnologies.garudasentinel.ui.components.ScreenGutter
import com.corbraytechnologies.garudasentinel.ui.components.ScreenTitle
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.utils.formatDuration

/** The Explore tab: the raw data the app has read, one row per source. */
@Composable
fun ExploreScreen(main: MainViewModel, onNavigate: (Dest) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Your data")
        Text(
            "Everything Garuda Sentinel has read from this phone. It stays here.",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.TextDim,
            modifier = Modifier.padding(horizontal = ScreenGutter),
        )
        Spacer(Modifier.height(16.dp))
        YourDataRows(main, onNavigate)
        Spacer(Modifier.height(24.dp))
    }
}

/** One row per data source with its count, shared by the Explore tab and the Report. */
@Composable
fun YourDataRows(main: MainViewModel, onNavigate: (Dest) -> Unit) {
    val context = LocalContext.current
    val apps by main.apps.collectAsStateWithLifecycle()
    val media by main.media.collectAsStateWithLifecycle()
    val files by main.files.collectAsStateWithLifecycle()
    val usage by main.usage.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (usage == null && Permissions.hasUsageAccess(context)) main.refreshUsage()
    }

    HairlineDivider()
    ListRow("Apps", value = apps.size.toString(), showChevron = true, onClick = { onNavigate(Dest.APPS) })
    HairlineDivider(inset = true)
    ListRow("Photos and media", value = media.size.toString(), showChevron = true, onClick = { onNavigate(Dest.MEDIA) })
    HairlineDivider(inset = true)
    ListRow(
        "Screen time",
        supporting = if (usage == null) "Needs Usage access" else "Today",
        value = usage?.let { formatDuration(it.screenOnTodayMs) },
        showChevron = true,
        onClick = { onNavigate(Dest.USAGE) },
    )
    HairlineDivider(inset = true)
    ListRow("Files", value = files.size.toString(), showChevron = true, onClick = { onNavigate(Dest.FILES) })
    HairlineDivider(inset = true)
    ListRow("Device and network", showChevron = true, onClick = { onNavigate(Dest.DEVICE) })
    HairlineDivider()
}
