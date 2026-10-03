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

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Photo dates: when the photo was taken (from EXIF) versus when the file was last written. */
object MediaDates {

    /**
     * Parses an EXIF date such as "2026:09:01 09:30:00". [offset] is the matching EXIF offset
     * tag ("+02:00"); without it the date is read in [zone], which is how cameras record it.
     * Returns null for missing, blank or all-zero values.
     */
    fun parseExif(value: String?, offset: String? = null, zone: TimeZone = TimeZone.getDefault()): Long? {
        val text = value?.trim()?.takeIf { it.length >= 19 && !it.startsWith("0000") } ?: return null
        val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
            isLenient = false
            timeZone = offsetZone(offset) ?: zone
        }
        return try {
            format.parse(text.substring(0, 19))?.time
        } catch (_: ParseException) {
            null
        }
    }

    private fun offsetZone(offset: String?): TimeZone? {
        val o = offset?.trim() ?: return null
        if (!Regex("""[+-]\d{2}:\d{2}""").matches(o)) return null
        return TimeZone.getTimeZone("GMT$o")
    }

    /** Short label for a list row: the capture date when known, otherwise a clearly marked file date. */
    fun label(dateTaken: Long?, fileDate: Long): String =
        if (dateTaken != null) "Taken ${formatDate(dateTaken)}" else "File date ${formatDate(fileDate)}"

    /**
     * The oldest date among [items] as (millis, isFileDate). Capture dates win; file dates are
     * used only when no item has a capture date.
     */
    fun <T> oldest(items: List<T>, dateTaken: (T) -> Long?, fileDate: (T) -> Long): Pair<Long, Boolean>? {
        items.mapNotNull(dateTaken).minOrNull()?.let { return it to false }
        return items.minOfOrNull(fileDate)?.let { it to true }
    }
}
