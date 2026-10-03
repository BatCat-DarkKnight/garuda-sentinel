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
import org.junit.Test

class SensitivePermissionsTest {

    private val usage = "android.permission.PACKAGE_USAGE_STATS"
    private val camera = "android.permission.CAMERA"
    private val mic = "android.permission.RECORD_AUDIO"

    private fun labels(list: List<SensitivePermissions.Info>) = list.map { it.label }

    @Test
    fun `runtime permissions follow the granted list`() {
        val b = SensitivePermissions.breakdown(listOf(camera, mic), granted = listOf(camera))
        assertEquals(listOf("Camera"), labels(b.allowed))
        assertEquals(listOf("Microphone"), labels(b.notAllowed))
        assertEquals(emptyList<String>(), labels(b.unknown))
    }

    @Test
    fun `usage access of another app is unknown, not "not allowed"`() {
        // The package manager never lists app-op permissions as granted.
        val b = SensitivePermissions.breakdown(listOf(usage), granted = emptyList())
        assertEquals(listOf("App usage"), labels(b.unknown))
        assertEquals(emptyList<String>(), labels(b.notAllowed))
    }

    @Test
    fun `usage access of this app uses the checked state`() {
        val on = SensitivePermissions.breakdown(listOf(usage), emptyList(), mapOf(usage to true))
        assertEquals(listOf("App usage"), labels(on.allowed))
        val off = SensitivePermissions.breakdown(listOf(usage), emptyList(), mapOf(usage to false))
        assertEquals(listOf("App usage"), labels(off.notAllowed))
    }

    @Test
    fun `non-sensitive permissions are ignored`() {
        val b = SensitivePermissions.breakdown(listOf("android.permission.VIBRATE"), listOf("android.permission.VIBRATE"))
        assertEquals(0, b.allowed.size + b.notAllowed.size + b.unknown.size)
    }
}
