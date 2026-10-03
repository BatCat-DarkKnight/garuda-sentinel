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

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListsTest {

    private fun app(pkg: String, system: Boolean) = AppMetadataEntity(
        packageName = pkg, appName = pkg, versionName = "1", versionCode = 1, firstInstallTime = 0,
        lastUpdateTime = 0, targetSdkVersion = 36, minSdkVersion = 29, permissions = "",
        grantedPermissions = "", isSystemApp = system, appCategory = "Other", installer = null,
    )

    @Test
    fun `installed by you leaves out system apps and Garuda Sentinel`() {
        val apps = listOf(app("com.example.notes", false), app("android", true), app("me.garuda", false))
        assertEquals(listOf("com.example.notes"), AppLists.userInstalled(apps, "me.garuda").map { it.packageName })
    }
}
