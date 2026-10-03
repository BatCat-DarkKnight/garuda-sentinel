package com.corbraytechnologies.garudasentinel.utils

import com.corbraytechnologies.garudasentinel.model.ScanSnapshot
import com.corbraytechnologies.garudasentinel.model.SnapshotApp
import com.corbraytechnologies.garudasentinel.model.SnapshotWatchers

/** An app that gained a sensitive permission since the previous scan. */
data class PermissionChange(val app: SnapshotApp, val permissionLabels: List<String>)

/** What changed between two scans. Empty when nothing worth reporting changed. */
data class ScanChanges(
    val previousAt: Long,
    val newApps: List<SnapshotApp> = emptyList(),
    val removedApps: List<SnapshotApp> = emptyList(),
    val newPermissions: List<PermissionChange> = emptyList(),
    val newWatcherSignals: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = newApps.isEmpty() && removedApps.isEmpty() && newPermissions.isEmpty() && newWatcherSignals.isEmpty()
}

/** Compares two scan snapshots. Pure, so it can be tested without a device. */
object ScanDiff {

    fun compute(previous: ScanSnapshot?, current: ScanSnapshot): ScanChanges? {
        if (previous == null) return null
        val before = previous.apps.associateBy { it.packageName }
        val after = current.apps.associateBy { it.packageName }

        val newApps = current.apps.filter { it.packageName !in before }
        val removedApps = previous.apps.filter { it.packageName !in after }

        val newPermissions = current.apps.mapNotNull { app ->
            val old = before[app.packageName] ?: return@mapNotNull null
            val gained = app.sensitivePermissions - old.sensitivePermissions.toSet()
            if (gained.isEmpty()) null else PermissionChange(app, gained.map { label(it) })
        }

        return ScanChanges(
            previousAt = previous.takenAt,
            newApps = newApps,
            removedApps = removedApps,
            newPermissions = newPermissions,
            newWatcherSignals = watcherChanges(previous.watchers, current.watchers, after, before),
        )
    }

    private fun watcherChanges(
        old: SnapshotWatchers,
        new: SnapshotWatchers,
        after: Map<String, SnapshotApp>,
        before: Map<String, SnapshotApp>,
    ): List<String> = buildList {
        fun added(older: List<String>, newer: List<String>) = newer - older.toSet()
        fun name(pkg: String) = after[pkg]?.appName ?: before[pkg]?.appName ?: pkg

        added(old.accessibilityServices, new.accessibilityServices).forEach {
            add("${name(it)} can now read your screen (accessibility service)")
        }
        added(old.notificationListeners, new.notificationListeners).forEach {
            add("${name(it)} can now read your notifications")
        }
        added(old.deviceAdmins, new.deviceAdmins).forEach {
            add("${name(it)} is now a device administrator")
        }
        added(old.backgroundLocationApps, new.backgroundLocationApps).forEach {
            add("${name(it)} can now use your location in the background")
        }
        added(old.appsWithoutLauncherIcon, new.appsWithoutLauncherIcon).forEach {
            add("${name(it)} has no icon in the app list")
        }
        if (new.userCaCertificateCount > old.userCaCertificateCount) {
            add("A certificate was added by hand (${new.userCaCertificateCount} in total)")
        }
        if (new.usbDebuggingEnabled && !old.usbDebuggingEnabled) add("USB debugging was turned on")
        if (!new.screenLockEnabled && old.screenLockEnabled) add("The screen lock was turned off")
    }

    private fun label(permission: String): String =
        SensitivePermissions.all[permission]?.label ?: permission.substringAfterLast('.')
}
