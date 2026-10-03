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

package com.corbraytechnologies.garudasentinel.findings

import com.corbraytechnologies.garudasentinel.utils.countOf

/** Small helpers that keep the finding sentences plain and consistent. */
object Wording {

    /** Apps are named in a sentence only when there are this many or fewer. */
    const val MAX_NAMES = 3

    /** "A", "A and B", "A, B and C". */
    fun list(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    /** The names when there are three or fewer, otherwise a count such as "5 apps". */
    fun subject(names: List<String>, noun: String): String =
        if (names.size in 1..MAX_NAMES) list(names) else countOf(names.size, noun)
}
