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
