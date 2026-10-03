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

package com.corbraytechnologies.garudasentinel.collect

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.corbraytechnologies.garudasentinel.data.FileMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Lists file metadata inside one folder the user picked with the system file picker
 * (Storage Access Framework). Android 11+ does not allow broad file listing without
 * "All files access", so this app only looks where the user explicitly points it.
 */
class FileCollector(private val context: Context) {

    data class Result(val items: List<FileMetadataEntity>, val notes: List<String>)

    fun hasAccess(treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }

    suspend fun collect(treeUri: Uri): Result = withContext(Dispatchers.IO) {
        if (!hasAccess(treeUri)) {
            return@withContext Result(emptyList(), listOf("Files skipped: access to the chosen folder was removed. Pick the folder again."))
        }
        val notes = mutableListOf<String>()
        val out = mutableListOf<FileMetadataEntity>()
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val queue = ArrayDeque(listOf(Triple(rootId, "", 0)))

        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (parentId, parentPath, depth) = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            context.contentResolver.query(childrenUri, COLUMNS, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.string(DocumentsContract.Document.COLUMN_DOCUMENT_ID) ?: continue
                    val name = c.string(DocumentsContract.Document.COLUMN_DISPLAY_NAME).orEmpty()
                    val mime = c.string(DocumentsContract.Document.COLUMN_MIME_TYPE).orEmpty()
                    val path = if (parentPath.isEmpty()) name else "$parentPath/$name"
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (depth < MAX_DEPTH) queue.addLast(Triple(id, path, depth + 1))
                        continue
                    }
                    if (out.size >= MAX_FILES) continue
                    out += FileMetadataEntity(
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString(),
                        fileName = name,
                        displayPath = path,
                        fileSize = c.long(DocumentsContract.Document.COLUMN_SIZE) ?: 0,
                        lastModified = c.long(DocumentsContract.Document.COLUMN_LAST_MODIFIED) ?: 0,
                        mimeType = mime.ifEmpty { "application/octet-stream" },
                        extension = name.substringAfterLast('.', "").lowercase(),
                    )
                }
            }
        }
        if (out.size >= MAX_FILES) notes += "Stopped after $MAX_FILES files."
        Result(out, notes)
    }

    private companion object {
        const val MAX_FILES = 5000
        const val MAX_DEPTH = 8
        val COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
