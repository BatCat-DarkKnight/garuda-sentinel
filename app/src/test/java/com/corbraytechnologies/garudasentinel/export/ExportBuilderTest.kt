package com.corbraytechnologies.garudasentinel.export

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.model.AppUsageInfo
import com.corbraytechnologies.garudasentinel.model.UsageSummary
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportBuilderTest {

    private val app = AppMetadataEntity(
        packageName = "com.example.maps",
        appName = "Maps",
        versionName = "1.0",
        versionCode = 1,
        firstInstallTime = 1,
        lastUpdateTime = 2,
        targetSdkVersion = 36,
        minSdkVersion = 29,
        permissions = "android.permission.CAMERA,android.permission.ACCESS_FINE_LOCATION",
        grantedPermissions = "android.permission.ACCESS_FINE_LOCATION",
        isSystemApp = false,
        appCategory = "Maps & Travel",
        installer = "com.android.vending",
    )

    private fun photo(uri: String, lat: Double?) = MediaMetadataEntity(
        contentUri = uri, fileName = "$uri.jpg", relativePath = "DCIM/Camera/", type = "image",
        fileSize = 10, lastModified = 1, mimeType = "image/jpeg", width = 1, height = 1, duration = null,
        dateTaken = 1, cameraMake = "Pixel", cameraModel = "9", gpsLat = lat, gpsLong = lat?.let { -it },
        orientation = null, iso = null, exposureTime = null, flash = null, bitrate = null,
        artist = null, album = null, genre = null, year = null, mediaCategory = "Camera",
    )

    private val usage = UsageSummary(
        collectedAt = 100, todayStart = 0, windowStart = -1, screenOnTodayMs = 42,
        apps = listOf(AppUsageInfo("com.example.maps", "Maps", 1, 2, 3, 4, 5, isBrowser = false)),
    )

    private fun sources(usage: UsageSummary? = this.usage) = ExportSources(
        device = null, battery = null, network = null, input = null,
        apps = listOf(app),
        usage = usage,
        media = listOf(photo("a", 1.5), photo("b", null)),
        files = emptyList(),
        locations = emptyList(),
        scanLogs = listOf(ScanLogEntity(1, 1, 2, 1, 2, 0, 1, "Files skipped: No folder chosen yet.\n")),
    )

    @Test
    fun `all categories are included by default`() {
        val doc = ExportBuilder.build(sources(), ExportCategory.entries.toSet(), "2.0.0", now = 7)
        assertEquals(ExportDocument.SCHEMA_VERSION, doc.schemaVersion)
        assertEquals(7L, doc.generatedAt)
        assertTrue(doc.excludedCategories.isEmpty())
        assertEquals(listOf("android.permission.ACCESS_FINE_LOCATION"), doc.apps!!.single().grantedPermissions)
        assertEquals(42L, doc.screenOnTodayMs)
        assertEquals(1, doc.appUsage!!.size)
        assertEquals(listOf("Files skipped: No folder chosen yet."), doc.scanHistory!!.single().notes)
    }

    @Test
    fun `excluded categories are null and listed`() {
        val doc = ExportBuilder.build(sources(), setOf(ExportCategory.APPS), "2.0.0", now = 7)
        assertNotNull(doc.apps)
        assertNull(doc.media)
        assertNull(doc.appUsage)
        assertNull(doc.scanHistory)
        assertTrue("MEDIA" in doc.excludedCategories)
        assertEquals(listOf("APPS"), doc.includedCategories)
    }

    @Test
    fun `photo gps is left out unless the user opts in`() {
        val doc = ExportBuilder.build(sources(), setOf(ExportCategory.MEDIA), "2.0.0", now = 7)
        val (withGps, withoutGps) = doc.media!!
        assertNull(withGps.exif!!.gpsLatitude)
        assertNull(withGps.exif!!.gpsLongitude)
        // The rest of the EXIF stays.
        assertEquals("Pixel", withGps.exif!!.cameraMake)
        assertNull(withoutGps.exif!!.gpsLatitude)
        assertEquals(false, doc.photoLocationsIncluded)
        assertTrue(doc.notes.any { "Photo GPS left out" in it })
    }

    @Test
    fun `photo gps is exported when the user opts in`() {
        val doc = ExportBuilder.build(sources(), setOf(ExportCategory.MEDIA), "2.0.0", now = 7, includePhotoLocations = true)
        val (withGps, withoutGps) = doc.media!!
        assertEquals(1.5, withGps.exif!!.gpsLatitude!!, 0.0)
        // Camera make is still EXIF, so the object exists even without GPS.
        assertNull(withoutGps.exif!!.gpsLatitude)
        assertEquals(true, doc.photoLocationsIncluded)
    }

    @Test
    fun `location readings are off by default`() {
        assertEquals(false, ExportCategory.LOCATION.onByDefault)
        assertEquals(true, ExportCategory.MEDIA.onByDefault)
    }

    @Test
    fun `missing usage access is explained in notes`() {
        val doc = ExportBuilder.build(sources(usage = null), setOf(ExportCategory.APP_USAGE), "2.0.0", now = 7)
        assertNull(doc.appUsage)
        assertTrue(doc.notes.any { "Usage access" in it })
    }

    @Test
    fun `encoded json is parseable and keeps null fields explicit`() {
        val text = ExportBuilder.encode(ExportBuilder.build(sources(), setOf(ExportCategory.APPS), "2.0.0", now = 7))
        val root = ExportBuilder.json.parseToJsonElement(text).jsonObject
        assertEquals("2", root["schemaVersion"]!!.jsonPrimitive.content)
        assertEquals("com.example.maps", root["apps"]!!.jsonArray[0].jsonObject["packageName"]!!.jsonPrimitive.content)
        assertTrue("media" in root)
    }
}
