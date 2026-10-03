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
