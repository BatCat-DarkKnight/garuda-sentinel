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

package com.corbraytechnologies.garudasentinel.model

import kotlinx.serialization.Serializable

/** One app as it looked at the time of a scan. */
@Serializable
data class SnapshotApp(
    val packageName: String,
    val appName: String,
    val installer: String? = null,
    /** Granted permissions that reveal personal data, by permission name. */
    val sensitivePermissions: List<String> = emptyList(),
)

/** The watcher signals worth comparing between scans, by package name. */
@Serializable
data class SnapshotWatchers(
    val accessibilityServices: List<String> = emptyList(),
    val notificationListeners: List<String> = emptyList(),
    val deviceAdmins: List<String> = emptyList(),
    val backgroundLocationApps: List<String> = emptyList(),
    val appsWithoutLauncherIcon: List<String> = emptyList(),
    val userCaCertificateCount: Int = 0,
    val usbDebuggingEnabled: Boolean = false,
    val screenLockEnabled: Boolean = true,
)

/**
 * A small record of each scan, kept so the app can say what changed since last time.
 * It holds no photo, file or location data.
 */
@Serializable
data class ScanSnapshot(
    val takenAt: Long,
    val apps: List<SnapshotApp> = emptyList(),
    val watchers: SnapshotWatchers = SnapshotWatchers(),
)
