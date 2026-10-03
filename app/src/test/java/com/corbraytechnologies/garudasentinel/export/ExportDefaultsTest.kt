package com.corbraytechnologies.garudasentinel.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Holds the export defaults still: a change here has to be a deliberate change to this test. */
class ExportDefaultsTest {

    @Test
    fun `every category except location readings is ticked by default`() {
        assertEquals(
            setOf(
                ExportCategory.DEVICE,
                ExportCategory.APPS,
                ExportCategory.APP_USAGE,
                ExportCategory.MEDIA,
                ExportCategory.FILES,
                ExportCategory.SCAN_HISTORY,
            ),
            ExportDefaults.categories,
        )
        assertFalse(ExportCategory.LOCATION in ExportDefaults.categories)
    }

    @Test
    fun `photo locations are left out by default`() {
        assertFalse(ExportDefaults.INCLUDE_PHOTO_LOCATIONS)
    }

    @Test
    fun `the password protected zip is selected by default`() {
        assertEquals(ExportFormat.ENCRYPTED_ZIP, ExportDefaults.format)
    }

    @Test
    fun `an export built with the defaults has no photo gps and no location readings`() {
        val doc = ExportBuilder.build(
            ExportSources(null, null, null, null, emptyList(), null, emptyList(), emptyList(), emptyList(), emptyList()),
            ExportDefaults.categories,
            appVersion = "2.1.0",
            now = 1,
            includePhotoLocations = ExportDefaults.INCLUDE_PHOTO_LOCATIONS,
        )
        assertFalse(doc.photoLocationsIncluded)
        assertEquals(null, doc.locations)
        assertEquals(listOf("LOCATION"), doc.excludedCategories)
    }
}
