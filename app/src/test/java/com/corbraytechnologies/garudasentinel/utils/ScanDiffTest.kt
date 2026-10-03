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

package com.corbraytechnologies.garudasentinel.utils

import com.corbraytechnologies.garudasentinel.model.ScanSnapshot
import com.corbraytechnologies.garudasentinel.model.SnapshotApp
import com.corbraytechnologies.garudasentinel.model.SnapshotWatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDiffTest {

    private val camera = "android.permission.CAMERA"
    private val mic = "android.permission.RECORD_AUDIO"

    private fun app(pkg: String, name: String = pkg, perms: List<String> = emptyList()) =
        SnapshotApp(packageName = pkg, appName = name, sensitivePermissions = perms)

    private fun snapshot(
        at: Long,
        apps: List<SnapshotApp> = emptyList(),
        watchers: SnapshotWatchers = SnapshotWatchers(),
    ) = ScanSnapshot(takenAt = at, apps = apps, watchers = watchers)

    @Test
    fun `the first scan has nothing to compare with`() {
        assertNull(ScanDiff.compute(previous = null, current = snapshot(2)))
    }

    @Test
    fun `an unchanged phone reports no changes`() {
        val apps = listOf(app("com.example.notes", "Notes", listOf(camera)))
        val changes = ScanDiff.compute(snapshot(1, apps), snapshot(2, apps))!!
        assertTrue(changes.isEmpty)
        assertEquals(1L, changes.previousAt)
    }

    @Test
    fun `installed and removed apps are listed`() {
        val changes = ScanDiff.compute(
            snapshot(1, listOf(app("com.example.old", "Old"))),
            snapshot(2, listOf(app("com.example.new", "New"))),
        )!!
        assertEquals(listOf("New"), changes.newApps.map { it.appName })
        assertEquals(listOf("Old"), changes.removedApps.map { it.appName })
        assertTrue(!changes.isEmpty)
    }

    @Test
    fun `newly granted sensitive permissions are listed with plain names`() {
        val changes = ScanDiff.compute(
            snapshot(1, listOf(app("com.example.chat", "Chat", listOf(camera)))),
            snapshot(2, listOf(app("com.example.chat", "Chat", listOf(camera, mic)))),
        )!!
        assertEquals(1, changes.newPermissions.size)
        assertEquals("Chat", changes.newPermissions[0].app.appName)
        assertEquals(listOf("Microphone"), changes.newPermissions[0].permissionLabels)
    }

    @Test
    fun `a permission that was taken away is not reported as new`() {
        val changes = ScanDiff.compute(
            snapshot(1, listOf(app("com.example.chat", "Chat", listOf(camera, mic)))),
            snapshot(2, listOf(app("com.example.chat", "Chat", listOf(camera)))),
        )!!
        assertTrue(changes.newPermissions.isEmpty())
        assertTrue(changes.isEmpty)
    }

    @Test
    fun `new watcher signals are named after the app`() {
        val apps = listOf(app("com.example.spy", "Helper"))
        val changes = ScanDiff.compute(
            snapshot(1, apps, SnapshotWatchers()),
            snapshot(
                2,
                apps,
                SnapshotWatchers(
                    accessibilityServices = listOf("com.example.spy"),
                    notificationListeners = listOf("com.example.spy"),
                    deviceAdmins = listOf("com.example.spy"),
                    backgroundLocationApps = listOf("com.example.spy"),
                    appsWithoutLauncherIcon = listOf("com.example.spy"),
                    userCaCertificateCount = 1,
                    usbDebuggingEnabled = true,
                    screenLockEnabled = false,
                ),
            ),
        )!!
        assertEquals(
            listOf(
                "Helper can now read your screen (accessibility service)",
                "Helper can now read your notifications",
                "Helper is now a device administrator",
                "Helper can now use your location in the background",
                "Helper has no icon in the app list",
                "A certificate was added by hand (1 in total)",
                "USB debugging was turned on",
                "The screen lock was turned off",
            ),
            changes.newWatcherSignals,
        )
    }

    @Test
    fun `a watcher signal that goes away is not reported`() {
        val changes = ScanDiff.compute(
            snapshot(1, watchers = SnapshotWatchers(accessibilityServices = listOf("com.example.spy"), usbDebuggingEnabled = true)),
            snapshot(2, watchers = SnapshotWatchers()),
        )!!
        assertTrue(changes.newWatcherSignals.isEmpty())
    }

    @Test
    fun `the name of an app that is gone still resolves from the previous scan`() {
        val changes = ScanDiff.compute(
            snapshot(1, listOf(app("com.example.spy", "Helper"))),
            snapshot(2, emptyList(), SnapshotWatchers(deviceAdmins = listOf("com.example.spy"))),
        )!!
        assertTrue(changes.newWatcherSignals.single().startsWith("Helper"))
    }
}
