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

import com.corbraytechnologies.garudasentinel.findings.Wording

/** The sentence above "Delete all scan data", built from what is actually stored. */
object DeleteSummary {

    /**
     * [folder] is the name of the folder the app can read, "" when the user picked the storage
     * root, or null when no folder was picked.
     */
    fun sentence(savedChecks: Int, locationReadings: Int, folder: String?): String {
        val parts = buildList {
            add(countOf(savedChecks, "saved check"))
            add(countOf(locationReadings, "location reading"))
            when {
                folder == null -> Unit
                folder.isBlank() -> add("access to the folder you chose")
                else -> add("access to your $folder folder")
            }
        }
        return "Removes " + Wording.list(parts) + ". Files you exported stay where you saved them."
    }
}
