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
