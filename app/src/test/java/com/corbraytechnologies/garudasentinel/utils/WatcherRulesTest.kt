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

import com.corbraytechnologies.garudasentinel.model.WatcherApp
import com.corbraytechnologies.garudasentinel.model.WatcherLevel
import com.corbraytechnologies.garudasentinel.model.WatcherSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatcherRulesTest {

    private fun app(name: String, system: Boolean = false) = WatcherApp(name, name, system)

    private fun titles(signals: WatcherSignals, level: WatcherLevel) =
        WatcherRules.findings(signals).filter { it.level == level }.map { it.title }

    @Test
    fun `a quiet phone has nothing to attend to`() {
        val signals = WatcherSignals(screenLockEnabled = true)
        assertEquals(0, WatcherRules.attentionCount(signals))
        assertTrue(titles(signals, WatcherLevel.FINE).contains("Screen lock is on"))
    }

    @Test
    fun `screen readers and other accessibility services need attention`() {
        val signals = WatcherSignals(accessibilityServices = listOf(app("com.example.spy")))
        assertEquals(listOf("Apps that can read your screen"), titles(signals, WatcherLevel.ATTENTION))
    }

    @Test
    fun `system accessibility services are only listed, not flagged`() {
        val signals = WatcherSignals(accessibilityServices = listOf(app("com.samsung.talkback", system = true)))
        assertEquals(0, WatcherRules.attentionCount(signals))
        assertTrue(titles(signals, WatcherLevel.FINE).contains("Accessibility services from your phone maker"))
    }

    @Test
    fun `notification access from outside a store, device admins and added certificates need attention`() {
        val signals = WatcherSignals(
            installedFromOutsideAStore = listOf(app("com.example.watch")),
            notificationListeners = listOf(app("com.example.watch")),
            deviceAdmins = listOf(app("com.example.admin")),
            userCaCertificates = listOf(app("user:1")),
        )
        assertEquals(3, WatcherRules.attentionCount(signals))
    }

    @Test
    fun `notification access from a store app is worth knowing, not attention`() {
        val signals = WatcherSignals(notificationListeners = listOf(app("com.example.buds")))
        assertEquals(0, WatcherRules.attentionCount(signals))
        assertEquals(listOf("Store apps that can read your notifications"), titles(signals, WatcherLevel.CHECK))
    }

    @Test
    fun `a missing screen lock needs attention`() {
        val signals = WatcherSignals(screenLockEnabled = false)
        assertTrue(titles(signals, WatcherLevel.ATTENTION).contains("No screen lock"))
    }

    @Test
    fun `background location, hidden apps, sideloading and usb debugging are worth a look`() {
        val signals = WatcherSignals(
            backgroundLocationApps = listOf(app("com.example.maps")),
            appsWithoutLauncherIcon = listOf(app("com.example.plugin")),
            installedFromOutsideAStore = listOf(app("com.example.sideloaded")),
            usbDebuggingEnabled = true,
            developerOptionsEnabled = true,
        )
        assertEquals(0, WatcherRules.attentionCount(signals))
        assertEquals(
            listOf(
                "Apps that can follow your location in the background",
                "Apps with no icon in the app list",
                "Apps installed from outside an app store",
                "Developer settings",
            ),
            titles(signals, WatcherLevel.CHECK),
        )
    }

    @Test
    fun `system apps are left out of the app based checks`() {
        val signals = WatcherSignals(
            backgroundLocationApps = listOf(app("com.android.gms", system = true)),
            appsWithoutLauncherIcon = listOf(app("com.android.service", system = true)),
            installedFromOutsideAStore = listOf(app("com.android.preinstalled", system = true)),
        )
        assertEquals(emptyList<String>(), titles(signals, WatcherLevel.CHECK))
    }

    @Test
    fun `signals that could not be read are reported, not guessed`() {
        val signals = WatcherSignals(unavailable = listOf("Certificates could not be read."))
        val finding = WatcherRules.findings(signals).single { it.title == "Not checked on this phone" }
        assertEquals(WatcherLevel.FINE, finding.level)
        assertTrue(finding.explanation.contains("Certificates"))
    }

    @Test
    fun `groups come back most serious first`() {
        val signals = WatcherSignals(
            accessibilityServices = listOf(app("com.example.spy")),
            usbDebuggingEnabled = true,
        )
        assertEquals(
            listOf(WatcherLevel.ATTENTION, WatcherLevel.CHECK, WatcherLevel.FINE),
            WatcherRules.grouped(signals).keys.toList(),
        )
    }
}
