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
