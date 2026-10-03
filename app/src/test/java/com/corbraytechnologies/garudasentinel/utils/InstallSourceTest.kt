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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallSourceTest {

    private val galaxyStore = "com.sec.android.app.samsungapps"
    private val budsManager = "com.samsung.accessory.budsunitemgr"

    @Test
    fun `an app installed by a store comes from a store`() {
        assertTrue(InstallSource.isFromStore("com.android.vending", installerOfInstaller = null, sameSigner = false))
    }

    @Test
    fun `a companion app installed by a same-signer app from a store is trusted`() {
        // Galaxy Buds2 Pro plugin, installed by Galaxy Buds Manager, which came from the Galaxy Store.
        assertTrue(InstallSource.isFromStore(budsManager, installerOfInstaller = galaxyStore, sameSigner = true))
    }

    @Test
    fun `an app installed by a store app from a different signer is still outside a store`() {
        assertFalse(InstallSource.isFromStore(budsManager, installerOfInstaller = galaxyStore, sameSigner = false))
    }

    @Test
    fun `a same-signer installer that did not come from a store is not enough`() {
        assertFalse(InstallSource.isFromStore("com.example.dropper", installerOfInstaller = null, sameSigner = true))
        assertFalse(InstallSource.isFromStore("com.example.dropper", installerOfInstaller = "com.example.other", sameSigner = true))
    }

    @Test
    fun `a plain file install is outside a store`() {
        assertFalse(InstallSource.isFromStore("com.google.android.packageinstaller", installerOfInstaller = null, sameSigner = false))
        assertFalse(InstallSource.isFromStore(null, installerOfInstaller = null, sameSigner = false))
    }

    @Test
    fun `labels name the store, the installing app, or a file`() {
        val names = mapOf(budsManager to "Galaxy Buds Manager")
        assertEquals("Installed by Galaxy Store", InstallSource.label(galaxyStore, names::get))
        assertEquals("Installed by Galaxy Buds Manager", InstallSource.label(budsManager, names::get))
        assertEquals("Installed by com.example.unknown", InstallSource.label("com.example.unknown", names::get))
        assertEquals("Installed from a file", InstallSource.label("com.google.android.packageinstaller", names::get))
        assertEquals("Installer unknown, or came with the phone", InstallSource.label(null, names::get))
    }
}
