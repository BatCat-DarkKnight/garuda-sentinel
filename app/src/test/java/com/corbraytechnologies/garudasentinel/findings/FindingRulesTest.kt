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

package com.corbraytechnologies.garudasentinel.findings

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.model.WatcherApp
import com.corbraytechnologies.garudasentinel.model.WatcherKind
import com.corbraytechnologies.garudasentinel.model.WatcherSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindingRulesTest {

    private val own = "com.corbraytechnologies.garudasentinel"

    private fun app(
        pkg: String,
        name: String = pkg.substringAfterLast('.'),
        system: Boolean = false,
        granted: List<String> = emptyList(),
        updated: Long = 0,
    ) = AppMetadataEntity(
        packageName = pkg, appName = name, versionName = "1", versionCode = 1,
        firstInstallTime = 0, lastUpdateTime = updated, targetSdkVersion = 36, minSdkVersion = 29,
        permissions = granted.joinToString(","), grantedPermissions = granted.joinToString(","),
        isSystemApp = system, appCategory = "Other", installer = null,
    )

    private fun ref(app: AppMetadataEntity) = WatcherApp(app.packageName, app.appName, app.isSystemApp)

    private fun photo(uri: String, lat: Double?, taken: Long = 1, type: String = "image") = MediaMetadataEntity(
        contentUri = uri, fileName = "$uri.jpg", relativePath = "DCIM/Camera/", type = type,
        fileSize = 1, lastModified = 1, mimeType = "image/jpeg", width = 1, height = 1, duration = null,
        dateTaken = taken, cameraMake = null, cameraModel = null, gpsLat = lat, gpsLong = lat,
        orientation = null, iso = null, exposureTime = null, flash = null, bitrate = null,
        artist = null, album = null, genre = null, year = null, mediaCategory = "Camera",
    )

    private fun build(
        apps: List<AppMetadataEntity> = emptyList(),
        media: List<MediaMetadataEntity> = emptyList(),
        signals: WatcherSignals = WatcherSignals(),
        photosReadable: Boolean = true,
        photoLocationsReadable: Boolean = true,
    ) = FindingRules.build(ReportInput(apps, own, media, signals, photosReadable, photoLocationsReadable))

    @Test
    fun `a quiet phone has no findings and counts its passed checks`() {
        val report = build(signals = WatcherSignals(screenLockEnabled = true))
        assertTrue(report.findings.isEmpty())
        assertEquals(1, report.passed)
        assertTrue(report.notChecked.isEmpty())
    }

    // HIGH: sideloaded app with notification access, accessibility or device admin

    @Test
    fun `a sideloaded app with notification access is high and opens its row`() {
        val spy = app("com.example.spy", "Sync Helper", updated = 50)
        val report = build(
            apps = listOf(spy),
            signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(spy)), notificationListeners = listOf(ref(spy))),
        )
        val finding = report.findings.first()
        assertEquals(FindingSeverity.HIGH, finding.severity)
        assertEquals("An app from outside a store can read your notifications", finding.title)
        assertEquals("Sync Helper came from outside an app store and has notification access.", finding.sentence)
        assertEquals(FindingAction.ReviewApp("com.example.spy"), finding.action)
        assertEquals("Review this app", finding.action.label)
        assertEquals(50L, finding.recency)
        // The same app is not reported again under the general notification access finding.
        assertEquals(1, report.findings.count { it.severity == FindingSeverity.HIGH })
    }

    @Test
    fun `the sideload title names the strongest capability and the sentence lists them all`() {
        val spy = app("com.example.spy", "Spy")
        val signals = WatcherSignals(
            installedFromOutsideAStore = listOf(ref(spy)),
            notificationListeners = listOf(ref(spy)),
            accessibilityServices = listOf(ref(spy)),
            deviceAdmins = listOf(ref(spy)),
        )
        val finding = build(apps = listOf(spy), signals = signals).findings.single { it.severity == FindingSeverity.HIGH }
        assertEquals("An app from outside a store can control your screen", finding.title)
        assertEquals(
            "Spy came from outside an app store and has an accessibility service turned on, " +
                "device administrator rights and notification access.",
            finding.sentence,
        )
    }

    @Test
    fun `a sideloaded device admin can manage this phone`() {
        val mdm = app("com.example.mdm", "Manager")
        val signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(mdm)), deviceAdmins = listOf(ref(mdm)))
        assertEquals(
            "An app from outside a store can manage this phone",
            build(apps = listOf(mdm), signals = signals).findings.first().title,
        )
    }

    @Test
    fun `a sideloaded app with none of the three is not high`() {
        val tool = app("com.example.tool")
        val signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(tool)))
        assertTrue(build(apps = listOf(tool), signals = signals).findings.none { it.severity == FindingSeverity.HIGH })
    }

    // HIGH: other attention watchers

    @Test
    fun `a store app with notification access is medium and opens the setting`() {
        val watch = app("com.example.watch", "Watch", updated = 12)
        val finding = build(apps = listOf(watch), signals = WatcherSignals(notificationListeners = listOf(ref(watch)))).findings.single()
        assertEquals(FindingSeverity.MEDIUM, finding.severity)
        assertEquals("1 app from a store can read your notifications", finding.title)
        assertEquals("Watch has notification access right now.", finding.sentence)
        assertEquals(FindingAction.OpenSetting(SettingsTarget.NOTIFICATION_ACCESS), finding.action)
        assertEquals("Open Android setting", finding.action.label)
        assertEquals(12L, finding.recency)
    }

    @Test
    fun `a same-signer companion counts as a store app, so its notification access is medium`() {
        // The collector leaves a same-signer companion of a store app out of installedFromOutsideAStore.
        val buds = app("com.samsung.accessory.zenithmgr", "Galaxy Buds2 Pro")
        val report = build(apps = listOf(buds), signals = WatcherSignals(notificationListeners = listOf(ref(buds))))
        assertEquals(listOf(FindingSeverity.MEDIUM), report.findings.map { it.severity })
        assertEquals("Galaxy Buds2 Pro has notification access right now.", report.findings.single().sentence)
    }

    @Test
    fun `notification access is high for an app from outside a store and medium for store apps`() {
        val spy = app("com.example.spy", "Spy")
        val watch = app("com.example.watch", "Watch")
        val signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(spy)), notificationListeners = listOf(ref(spy), ref(watch)))
        val findings = build(apps = listOf(spy, watch), signals = signals).findings
        assertEquals("An app from outside a store can read your notifications", findings.single { it.severity == FindingSeverity.HIGH }.title)
        assertEquals("1 app from a store can read your notifications", findings.single { it.severity == FindingSeverity.MEDIUM }.title)
        assertEquals(2, findings.size)
    }

    @Test
    fun `a certificate added by hand is high`() {
        val cert = WatcherApp("user:abc", "Example CA", isSystemApp = false, detail = "Added by hand")
        val finding = build(signals = WatcherSignals(userCaCertificates = listOf(cert))).findings.single()
        assertEquals(FindingSeverity.HIGH, finding.severity)
        assertEquals("Certificates added by someone", finding.title)
        assertEquals("Example CA, added by hand, can let whoever issued it read traffic from some apps.", finding.sentence)
        assertEquals(FindingAction.OpenSetting(SettingsTarget.SECURITY), finding.action)
    }

    @Test
    fun `no screen lock is high`() {
        val finding = build(signals = WatcherSignals(screenLockEnabled = false)).findings.single()
        assertEquals(FindingSeverity.HIGH, finding.severity)
        assertEquals("No screen lock", finding.title)
        assertEquals(FindingAction.OpenSetting(SettingsTarget.SECURITY), finding.action)
    }

    @Test
    fun `accessibility from a store app is high, apart from the sideloaded one already reported`() {
        val spy = app("com.example.spy", "Spy")
        val reader = app("com.example.reader", "Reader")
        val signals = WatcherSignals(
            installedFromOutsideAStore = listOf(ref(spy)),
            accessibilityServices = listOf(ref(spy), ref(reader)),
        )
        val high = build(apps = listOf(spy, reader), signals = signals).findings.filter { it.severity == FindingSeverity.HIGH }
        assertEquals(2, high.size)
        assertEquals("Reader can see what is on screen and what you type.", high.single { it.title == "Apps that can read your screen" }.sentence)
    }

    // MEDIUM: photos with GPS

    @Test
    fun `photos with gps are medium and counted`() {
        val media = listOf(photo("a", 1.0, taken = 10), photo("b", 2.0, taken = 30), photo("c", null), photo("v", 3.0, type = "video"))
        val finding = build(media = media).findings.single()
        assertEquals(FindingSeverity.MEDIUM, finding.severity)
        assertEquals("2 photos show exactly where they were taken", finding.title)
        assertEquals(FindingAction.SeePlaces, finding.action)
        assertEquals("See the places", finding.action.label)
        assertEquals(30L, finding.recency)
    }

    @Test
    fun `one located photo reads in the singular`() {
        assertEquals("1 photo shows exactly where it was taken", build(media = listOf(photo("a", 1.0))).findings.single().title)
    }

    @Test
    fun `photos are not judged when they could not be read`() {
        val report = build(media = listOf(photo("a", 1.0)), photosReadable = false)
        assertTrue(report.findings.isEmpty())
        assertEquals("Photos were not checked", report.notChecked.single().title)
        assertEquals(FindingAction.SetUp, report.notChecked.single().action)
        assertEquals("Set up", report.notChecked.single().action.label)
    }

    @Test
    fun `hidden photo locations get a neutral row`() {
        val report = build(photoLocationsReadable = false)
        assertEquals("Photo locations were not checked", report.notChecked.single().title)
    }

    // MEDIUM: microphone, camera and precise location

    @Test
    fun `one finding per sensitive permission, naming up to three apps`() {
        val apps = listOf(
            app("com.a", "Alpha", granted = listOf(FindingRules.RECORD_AUDIO, FindingRules.CAMERA)),
            app("com.b", "Bravo", granted = listOf(FindingRules.RECORD_AUDIO)),
        )
        val findings = build(apps = apps).findings
        val mic = findings.single { it.title == "2 apps you installed can use your microphone" }
        assertEquals("Alpha and Bravo have this permission right now.", mic.sentence)
        assertEquals(FindingAction.SeeAppsByAccess, mic.action)
        assertEquals("See which apps", mic.action.label)
        val camera = findings.single { it.title == "1 app you installed can use your camera" }
        assertEquals("Alpha has this permission right now.", camera.sentence)
        assertTrue(findings.none { "precise location" in it.title })
    }

    @Test
    fun `more than three apps are counted, not named`() {
        val apps = (1..4).map { app("com.app$it", "App $it", granted = listOf(FindingRules.FINE_LOCATION)) }
        val finding = build(apps = apps).findings.single()
        assertEquals("4 apps you installed can use your precise location", finding.title)
        assertEquals("Check that each one still needs it.", finding.sentence)
    }

    @Test
    fun `system apps and Garuda Sentinel itself are left out of the permission findings`() {
        val apps = listOf(
            app("com.android.camera", system = true, granted = listOf(FindingRules.CAMERA)),
            app(own, "Garuda Sentinel", granted = listOf(FindingRules.CAMERA)),
        )
        assertTrue(build(apps = apps).findings.isEmpty())
    }

    // MEDIUM: background location

    @Test
    fun `background location for an installed app is medium, not also low`() {
        val maps = app("com.example.maps", "Maps")
        val system = app("com.android.gms", system = true)
        val findings = build(apps = listOf(maps, system), signals = WatcherSignals(backgroundLocationApps = listOf(ref(maps), ref(system)))).findings
        val finding = findings.single()
        assertEquals(FindingSeverity.MEDIUM, finding.severity)
        assertEquals("1 app can see where you are while closed", finding.title)
        assertEquals("Maps has background location right now.", finding.sentence)
        assertEquals(FindingAction.SeeAppsByAccess, finding.action)
    }

    // LOW: USB debugging

    @Test
    fun `usb debugging is low and replaces the developer settings finding`() {
        val findings = build(signals = WatcherSignals(usbDebuggingEnabled = true, developerOptionsEnabled = true)).findings
        val finding = findings.single()
        assertEquals(FindingSeverity.LOW, finding.severity)
        assertEquals("USB debugging is on", finding.title)
        assertEquals(FindingAction.OpenSetting(SettingsTarget.DEVELOPER_OPTIONS, "Open developer options"), finding.action)
    }

    // LOW: other worth-knowing watchers

    @Test
    fun `developer options without usb debugging is low with its watcher title`() {
        val finding = build(signals = WatcherSignals(developerOptionsEnabled = true)).findings.single()
        assertEquals(FindingSeverity.LOW, finding.severity)
        assertEquals("Developer settings", finding.title)
        assertEquals(FindingAction.OpenSetting(SettingsTarget.DEVELOPER_OPTIONS), finding.action)
    }

    @Test
    fun `a single hidden app opens its App info`() {
        val plugin = app("com.example.plugin", "Plugin")
        val finding = build(apps = listOf(plugin), signals = WatcherSignals(appsWithoutLauncherIcon = listOf(ref(plugin)))).findings.single()
        assertEquals(FindingSeverity.LOW, finding.severity)
        assertEquals("Apps with no icon in the app list", finding.title)
        assertEquals("Plugin has no icon in the app list.", finding.sentence)
        assertEquals(FindingAction.OpenAppInfo("com.example.plugin"), finding.action)
    }

    @Test
    fun `several sideloaded apps open the app list, and Garuda Sentinel is not one of them`() {
        val a = app("com.a", "Alpha")
        val b = app("com.b", "Bravo")
        val self = app(own, "Garuda Sentinel")
        val finding = build(apps = listOf(a, b, self), signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(a), ref(b), ref(self)))).findings.single()
        assertEquals("Apps installed from outside an app store", finding.title)
        assertEquals("Alpha and Bravo came from a file or another app, not a store.", finding.sentence)
        assertEquals(FindingAction.OpenSetting(SettingsTarget.ALL_APPS), finding.action)
    }

    @Test
    fun `Garuda Sentinel alone being sideloaded is not a finding`() {
        val self = app(own, "Garuda Sentinel")
        assertTrue(build(apps = listOf(self), signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(self)))).findings.isEmpty())
    }

    @Test
    fun `an app already reported as high is not listed again as sideloaded`() {
        val spy = app("com.example.spy", "Spy")
        val signals = WatcherSignals(installedFromOutsideAStore = listOf(ref(spy)), notificationListeners = listOf(ref(spy)))
        assertTrue(build(apps = listOf(spy), signals = signals).findings.none { it.severity == FindingSeverity.LOW })
    }

    // Passed and not checked

    @Test
    fun `all clear watchers count as passed, but signals that could not be read do not`() {
        val sms = WatcherApp("com.android.messaging", "Messages", isSystemApp = true)
        val talkback = WatcherApp("com.google.talkback", "TalkBack", isSystemApp = true)
        val report = build(
            signals = WatcherSignals(
                defaultSmsApp = sms,
                accessibilityServices = listOf(talkback),
                screenLockEnabled = true,
                unavailable = listOf("Certificates could not be read."),
            ),
        )
        assertEquals(3, report.passed)
        val row = report.notChecked.single()
        assertEquals("1 setting could not be read", row.title)
        assertEquals(FindingAction.SeeWhoCanWatch, row.action)
    }

    // Ranking

    @Test
    fun `findings rank high, then medium, then low, most recent first within a severity`() {
        val old = app("com.old", "Old", granted = listOf(FindingRules.CAMERA), updated = 1)
        val new = app("com.new", "New", granted = listOf(FindingRules.RECORD_AUDIO), updated = 9)
        val report = build(
            apps = listOf(old, new),
            media = listOf(photo("a", 1.0, taken = 5)),
            signals = WatcherSignals(usbDebuggingEnabled = true, screenLockEnabled = false),
        )
        assertEquals(
            listOf(
                "No screen lock",
                "1 app you installed can use your microphone",
                "1 photo shows exactly where it was taken",
                "1 app you installed can use your camera",
                "USB debugging is on",
            ),
            report.findings.map { it.title },
        )
        assertEquals(1, report.count(FindingSeverity.HIGH))
        assertEquals(3, report.count(FindingSeverity.MEDIUM))
        assertEquals(1, report.count(FindingSeverity.LOW))
    }

    @Test
    fun `every watcher with a setting maps to a settings screen`() {
        WatcherKind.entries.forEach { kind ->
            val target = FindingRules.settingsTarget(kind)
            if (kind == WatcherKind.NOT_CHECKED) {
                assertEquals(null, target)
            } else {
                assertTrue("$kind has no settings screen", target != null)
            }
        }
    }
}
