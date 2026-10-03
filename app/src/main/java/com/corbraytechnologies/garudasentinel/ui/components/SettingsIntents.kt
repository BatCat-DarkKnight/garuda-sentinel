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

package com.corbraytechnologies.garudasentinel.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.corbraytechnologies.garudasentinel.findings.SettingsTarget

/**
 * The standard Settings intent for each screen a finding can open. These only open a screen;
 * any change is made by the user there. [openSettings] falls back to the main Settings screen
 * when a phone does not have one of these.
 */
fun SettingsTarget.intent(): Intent = Intent(
    when (this) {
        SettingsTarget.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
        SettingsTarget.NOTIFICATION_ACCESS -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        SettingsTarget.SECURITY -> Settings.ACTION_SECURITY_SETTINGS
        SettingsTarget.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
        SettingsTarget.DEVELOPER_OPTIONS -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        SettingsTarget.ALL_APPS -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
        SettingsTarget.DEFAULT_APPS -> Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
    },
)

/** The system App info screen for one app. */
fun appInfoIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
