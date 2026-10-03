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

/** An app named in a watcher signal. */
@Serializable
data class WatcherApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    /** Extra detail for the row, such as a certificate name or an installer. */
    val detail: String? = null,
)

/**
 * Everything the watchers check reads, before any judgement is applied.
 * Collected from public APIs only; see WatcherCollector.
 */
@Serializable
data class WatcherSignals(
    val accessibilityServices: List<WatcherApp> = emptyList(),
    val notificationListeners: List<WatcherApp> = emptyList(),
    val deviceAdmins: List<WatcherApp> = emptyList(),
    val backgroundLocationApps: List<WatcherApp> = emptyList(),
    val installedFromOutsideAStore: List<WatcherApp> = emptyList(),
    val appsWithoutLauncherIcon: List<WatcherApp> = emptyList(),
    val userCaCertificates: List<WatcherApp> = emptyList(),
    val defaultSmsApp: WatcherApp? = null,
    val screenLockEnabled: Boolean = true,
    val developerOptionsEnabled: Boolean = false,
    val usbDebuggingEnabled: Boolean = false,
    /** Signals that could not be read on this device, with the reason. */
    val unavailable: List<String> = emptyList(),
)

/** How much attention a finding deserves. */
enum class WatcherLevel { ATTENTION, CHECK, FINE }

/** Which check produced a finding, so the Report and the settings links can tell them apart. */
enum class WatcherKind {
    ACCESSIBILITY,
    SYSTEM_ACCESSIBILITY,
    NOTIFICATIONS,
    NOTIFICATIONS_FROM_STORE,
    DEVICE_ADMIN,
    CERTIFICATES,
    NO_SCREEN_LOCK,
    BACKGROUND_LOCATION,
    HIDDEN_APPS,
    SIDELOADED,
    DEVELOPER,
    DEFAULT_SMS,
    SCREEN_LOCK_ON,
    NOT_CHECKED,
}

/** One line of the "Who can watch" screen. */
data class WatcherFinding(
    val level: WatcherLevel,
    val title: String,
    val explanation: String,
    val apps: List<WatcherApp> = emptyList(),
    val kind: WatcherKind,
)
