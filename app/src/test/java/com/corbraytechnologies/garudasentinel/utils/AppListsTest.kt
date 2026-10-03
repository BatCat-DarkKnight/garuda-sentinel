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
