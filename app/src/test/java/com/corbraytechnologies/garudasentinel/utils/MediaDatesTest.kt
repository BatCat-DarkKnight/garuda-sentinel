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
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class MediaDatesTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `exif dates are read in the given zone`() {
        // 2026-09-01 09:30:00 UTC
        assertEquals(1788255000000L, MediaDates.parseExif("2026:09:01 09:30:00", zone = utc))
    }

    @Test
    fun `an exif offset wins over the zone`() {
        // 09:30 at +02:00 is 07:30 UTC.
        assertEquals(1788255000000L - 2 * 3_600_000L, MediaDates.parseExif("2026:09:01 09:30:00", "+02:00", utc))
    }

    @Test
    fun `missing, blank, zero and malformed dates are null`() {
        assertNull(MediaDates.parseExif(null))
        assertNull(MediaDates.parseExif("   "))
        assertNull(MediaDates.parseExif("0000:00:00 00:00:00"))
        assertNull(MediaDates.parseExif("2026-09-01", zone = utc))
        assertNull(MediaDates.parseExif("2026:13:45 99:99:99", zone = utc))
    }

    @Test
    fun `labels say whether the date is a capture date or a file date`() {
        assertEquals(true, MediaDates.label(0L, 5L).startsWith("Taken "))
        assertEquals(true, MediaDates.label(null, 5L).startsWith("File date "))
    }

    @Test
    fun `oldest prefers capture dates and falls back to file dates`() {
        data class P(val taken: Long?, val file: Long)
        assertEquals(300L to false, MediaDates.oldest(listOf(P(null, 10), P(300, 50), P(400, 5)), { it.taken }, { it.file }))
        assertEquals(5L to true, MediaDates.oldest(listOf(P(null, 10), P(null, 5)), { it.taken }, { it.file }))
        assertNull(MediaDates.oldest(emptyList<P>(), { it.taken }, { it.file }))
    }
}
