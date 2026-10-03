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
