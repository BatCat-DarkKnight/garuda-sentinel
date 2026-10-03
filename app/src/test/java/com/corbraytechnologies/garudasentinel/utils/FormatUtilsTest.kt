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
import java.util.Locale

class FormatUtilsTest {

    @Test
    fun `durations are compact`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("40s", formatDuration(40_000))
        assertEquals("12m", formatDuration(12 * 60_000L))
        assertEquals("2h 05m", formatDuration((2 * 60 + 5) * 60_000L))
        assertEquals("0s", formatDuration(-5))
    }

    @Test
    fun `file sizes use binary units`() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("0 B", formatFileSize(0))
            assertEquals("512 B", formatFileSize(512))
            assertEquals("1.5 KB", formatFileSize(1536))
            assertEquals("1.0 GB", formatFileSize(1L shl 30))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `detail lines skip blank parts`() {
        assertEquals("sub  |  7 B  |  today", detailLine("sub", "7 B", "today"))
        assertEquals("8 B  |  today", detailLine("", "8 B", "today"))
        assertEquals("8 B", detailLine(null, "8 B", " "))
    }

    @Test
    fun `counts use singular and plural`() {
        assertEquals("1 app", countOf(1, "app"))
        assertEquals("0 apps", countOf(0, "app"))
        assertEquals("3 media", countOf(3, "media", "media"))
    }
}
