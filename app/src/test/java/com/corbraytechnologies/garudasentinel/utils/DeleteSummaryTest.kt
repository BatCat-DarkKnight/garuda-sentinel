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

class DeleteSummaryTest {

    @Test
    fun `names the counts and the chosen folder`() {
        assertEquals(
            "Removes 3 saved checks, 2 location readings and access to your Download folder. " +
                "Files you exported stay where you saved them.",
            DeleteSummary.sentence(3, 2, "Download"),
        )
    }

    @Test
    fun `singular counts and no folder`() {
        assertEquals(
            "Removes 1 saved check and 1 location reading. Files you exported stay where you saved them.",
            DeleteSummary.sentence(1, 1, null),
        )
    }

    @Test
    fun `the storage root has no folder name`() {
        assertEquals(
            "Removes 0 saved checks, 0 location readings and access to the folder you chose. " +
                "Files you exported stay where you saved them.",
            DeleteSummary.sentence(0, 0, ""),
        )
    }
}
