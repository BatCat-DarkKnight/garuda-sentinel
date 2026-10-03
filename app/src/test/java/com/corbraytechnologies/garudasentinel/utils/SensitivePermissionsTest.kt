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
