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
