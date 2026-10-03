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

/** The kind of file an export writes. */
enum class ExportFormat { ENCRYPTED_ZIP, PLAIN_JSON }

/**
 * What the export form starts with. Kept in one place so a unit test can hold them still:
 * photo GPS and location readings are off, and the password-protected ZIP is selected.
 */
object ExportDefaults {
    val categories: Set<ExportCategory> = ExportCategory.entries.filter { it.onByDefault }.toSet()
    const val INCLUDE_PHOTO_LOCATIONS = false
    val format = ExportFormat.ENCRYPTED_ZIP
}
