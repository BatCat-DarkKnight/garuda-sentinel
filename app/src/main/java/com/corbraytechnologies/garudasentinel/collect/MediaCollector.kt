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

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.permissions.MediaAccess
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.utils.AppCategorizer
import com.corbraytechnologies.garudasentinel.utils.MediaDates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext

/**
 * Reads photo, video and audio metadata from MediaStore. File contents are only
 * opened to read EXIF headers; nothing is copied or uploaded.
 */
class MediaCollector(private val context: Context) {

    /** [permitted] is false when no media permission at all was granted, so nothing could be read. */
    data class Result(val items: List<MediaMetadataEntity>, val notes: List<String>, val permitted: Boolean)

    suspend fun collect(): Result = withContext(Dispatchers.IO) {
        val notes = mutableListOf<String>()
        val items = mutableListOf<MediaMetadataEntity>()

        when (Permissions.mediaAccess(context)) {
            MediaAccess.NONE -> notes += "Photos and videos skipped: media permission not granted."
            MediaAccess.PARTIAL -> {
                notes += "Only the photos and videos you selected were scanned (limited access)."
                items += images(notes)
                items += videos()
            }
            MediaAccess.FULL -> {
                items += images(notes)
                items += videos()
            }
        }
        val audioAllowed = Permissions.hasAudioAccess(context)
        if (audioAllowed) {
            items += audio()
        } else {
            notes += "Audio skipped: music and audio permission not granted."
        }
        Result(items, notes, permitted = audioAllowed || Permissions.mediaAccess(context) != MediaAccess.NONE)
    }

    private suspend fun images(notes: MutableList<String>): List<MediaMetadataEntity> {
        val canReadLocation = Permissions.hasMediaLocation(context)
        if (!canReadLocation) {
            notes += "Photo GPS not read: Android hides it unless photo location access is granted."
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
        )
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return query(collection, projection) { c ->
            val uri = ContentUris.withAppendedId(collection, c.long(MediaStore.MediaColumns._ID)!!)
            val exif = readExif(uri, canReadLocation)
            val latLong = exif?.latLong
            val name = c.string(MediaStore.MediaColumns.DISPLAY_NAME).orEmpty()
            val path = c.string(MediaStore.MediaColumns.RELATIVE_PATH).orEmpty()
            MediaMetadataEntity(
                contentUri = uri.toString(),
                fileName = name,
                relativePath = path,
                type = TYPE_IMAGE,
                fileSize = c.long(MediaStore.MediaColumns.SIZE) ?: 0,
                lastModified = (c.long(MediaStore.MediaColumns.DATE_MODIFIED) ?: 0) * 1000,
                mimeType = c.string(MediaStore.MediaColumns.MIME_TYPE) ?: "image/*",
                width = c.int(MediaStore.MediaColumns.WIDTH)?.takeIf { it > 0 },
                height = c.int(MediaStore.MediaColumns.HEIGHT)?.takeIf { it > 0 },
                duration = null,
                // MediaStore's DATE_TAKEN can silently fall back to the file time, so photos use
                // the EXIF capture date and leave it empty when the photo has none.
                dateTaken = exif?.let(::exifCaptureDate),
                cameraMake = exif?.getAttribute(ExifInterface.TAG_MAKE),
                cameraModel = exif?.getAttribute(ExifInterface.TAG_MODEL),
                gpsLat = latLong?.getOrNull(0),
                gpsLong = latLong?.getOrNull(1),
                orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)?.takeIf { it > 0 },
                iso = exif?.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)?.takeIf { it > 0 },
                exposureTime = exif?.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                flash = exif?.getAttribute(ExifInterface.TAG_FLASH),
                bitrate = null,
                artist = null,
                album = null,
                genre = null,
                year = null,
                mediaCategory = AppCategorizer.categorizeMediaFile(path, name),
            )
        }
    }

    private suspend fun videos(): List<MediaMetadataEntity> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
        )
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        return query(collection, projection) { c ->
            val name = c.string(MediaStore.MediaColumns.DISPLAY_NAME).orEmpty()
            val path = c.string(MediaStore.MediaColumns.RELATIVE_PATH).orEmpty()
            MediaMetadataEntity(
                contentUri = ContentUris.withAppendedId(collection, c.long(MediaStore.MediaColumns._ID)!!).toString(),
                fileName = name,
                relativePath = path,
                type = TYPE_VIDEO,
                fileSize = c.long(MediaStore.MediaColumns.SIZE) ?: 0,
                lastModified = (c.long(MediaStore.MediaColumns.DATE_MODIFIED) ?: 0) * 1000,
                mimeType = c.string(MediaStore.MediaColumns.MIME_TYPE) ?: "video/*",
                width = c.int(MediaStore.MediaColumns.WIDTH)?.takeIf { it > 0 },
                height = c.int(MediaStore.MediaColumns.HEIGHT)?.takeIf { it > 0 },
                duration = c.long(MediaStore.MediaColumns.DURATION)?.takeIf { it > 0 },
                dateTaken = c.long(MediaStore.MediaColumns.DATE_TAKEN)?.takeIf { it > 0 },
                cameraMake = null, cameraModel = null, gpsLat = null, gpsLong = null,
                orientation = null, iso = null, exposureTime = null, flash = null,
                bitrate = null, artist = null, album = null, genre = null, year = null,
                mediaCategory = AppCategorizer.categorizeMediaFile(path, name),
            )
        }
    }

    private suspend fun audio(): List<MediaMetadataEntity> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.DURATION)
            add(MediaStore.Audio.AudioColumns.ARTIST)
            add(MediaStore.Audio.AudioColumns.ALBUM)
            add(MediaStore.Audio.AudioColumns.YEAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(MediaStore.Audio.AudioColumns.GENRE)
        }.toTypedArray()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        return query(collection, projection) { c ->
            val name = c.string(MediaStore.MediaColumns.DISPLAY_NAME).orEmpty()
            val path = c.string(MediaStore.MediaColumns.RELATIVE_PATH).orEmpty()
            MediaMetadataEntity(
                contentUri = ContentUris.withAppendedId(collection, c.long(MediaStore.MediaColumns._ID)!!).toString(),
                fileName = name,
                relativePath = path,
                type = TYPE_AUDIO,
                fileSize = c.long(MediaStore.MediaColumns.SIZE) ?: 0,
                lastModified = (c.long(MediaStore.MediaColumns.DATE_MODIFIED) ?: 0) * 1000,
                mimeType = c.string(MediaStore.MediaColumns.MIME_TYPE) ?: "audio/*",
                width = null, height = null,
                duration = c.long(MediaStore.MediaColumns.DURATION)?.takeIf { it > 0 },
                dateTaken = null,
                cameraMake = null, cameraModel = null, gpsLat = null, gpsLong = null,
                orientation = null, iso = null, exposureTime = null, flash = null, bitrate = null,
                artist = c.string(MediaStore.Audio.AudioColumns.ARTIST)?.takeUnless { it == MediaStore.UNKNOWN_STRING },
                album = c.string(MediaStore.Audio.AudioColumns.ALBUM),
                genre = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) c.string(MediaStore.Audio.AudioColumns.GENRE) else null,
                year = c.int(MediaStore.Audio.AudioColumns.YEAR)?.takeIf { it > 0 },
                mediaCategory = AppCategorizer.categorizeMediaFile(path, name),
            )
        }
    }

    private fun exifCaptureDate(exif: ExifInterface): Long? =
        MediaDates.parseExif(
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
        ) ?: MediaDates.parseExif(
            exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED),
            exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED),
        )

    private fun readExif(uri: Uri, withLocation: Boolean): ExifInterface? = try {
        val source = if (withLocation) MediaStore.setRequireOriginal(uri) else uri
        context.contentResolver.openInputStream(source)?.use { ExifInterface(it) }
    } catch (_: Exception) {
        null
    }

    private suspend fun query(
        collection: Uri,
        projection: Array<String>,
        map: (Cursor) -> MediaMetadataEntity,
    ): List<MediaMetadataEntity> {
        val out = mutableListOf<MediaMetadataEntity>()
        context.contentResolver.query(collection, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                runCatching { map(c) }.onSuccess { out += it }
            }
        }
        return out
    }

    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
    }
}

internal fun Cursor.string(column: String): String? =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let { getString(it) }

internal fun Cursor.long(column: String): Long? =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let { getLong(it) }

internal fun Cursor.int(column: String): Int? =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let { getInt(it) }
