package com.corbraytechnologies.garudasentinel.data

import android.content.Context
import java.io.File

/**
 * "Forget results when I close the app": when on, the database lives in memory and disappears when the app
 * process ends. The flag is a marker file in no-backup storage, so it can be read synchronously
 * before the database is opened and is never backed up.
 */
object StorageMode {
    private const val MARKER = "memory_only"

    private fun marker(context: Context) = File(context.noBackupFilesDir, MARKER)

    fun isMemoryOnly(context: Context): Boolean = marker(context).exists()

    fun setMemoryOnly(context: Context, on: Boolean) {
        val file = marker(context)
        if (on) file.createNewFile() else file.delete()
    }
}
