package com.corbraytechnologies.garudasentinel.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "version_name") val versionName: String,
    @ColumnInfo(name = "version_code") val versionCode: Long,
    @ColumnInfo(name = "first_install_time") val firstInstallTime: Long,
    @ColumnInfo(name = "last_update_time") val lastUpdateTime: Long,
    @ColumnInfo(name = "target_sdk") val targetSdkVersion: Int,
    @ColumnInfo(name = "min_sdk") val minSdkVersion: Int,
    /** Requested permissions, comma separated. */
    @ColumnInfo(name = "permissions") val permissions: String,
    /** The subset of [permissions] currently granted, comma separated. */
    @ColumnInfo(name = "granted_permissions") val grantedPermissions: String,
    @ColumnInfo(name = "is_system_app") val isSystemApp: Boolean,
    @ColumnInfo(name = "category") val appCategory: String,
    /** Package that installed this app (for example com.android.vending), or null if unknown. */
    @ColumnInfo(name = "installer") val installer: String?,
) {
    val permissionList: List<String>
        get() = if (permissions.isBlank()) emptyList() else permissions.split(",")

    val grantedPermissionList: List<String>
        get() = if (grantedPermissions.isBlank()) emptyList() else grantedPermissions.split(",")
}

@Entity(tableName = "media")
data class MediaMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    /** Folder the file lives in, relative to shared storage (for example DCIM/Camera/). */
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "width") val width: Int?,
    @ColumnInfo(name = "height") val height: Int?,
    @ColumnInfo(name = "duration_ms") val duration: Long?,
    @ColumnInfo(name = "date_taken") val dateTaken: Long?,
    @ColumnInfo(name = "camera_make") val cameraMake: String?,
    @ColumnInfo(name = "camera_model") val cameraModel: String?,
    @ColumnInfo(name = "gps_lat") val gpsLat: Double?,
    @ColumnInfo(name = "gps_long") val gpsLong: Double?,
    @ColumnInfo(name = "orientation") val orientation: Int?,
    @ColumnInfo(name = "iso") val iso: Int?,
    @ColumnInfo(name = "exposure_time") val exposureTime: String?,
    @ColumnInfo(name = "flash") val flash: String?,
    @ColumnInfo(name = "bitrate") val bitrate: Int?,
    @ColumnInfo(name = "artist") val artist: String?,
    @ColumnInfo(name = "album") val album: String?,
    @ColumnInfo(name = "genre") val genre: String?,
    @ColumnInfo(name = "year") val year: Int?,
    @ColumnInfo(name = "source") val mediaCategory: String,
)

/** A file found in a folder the user picked through the system file picker. */
@Entity(tableName = "files")
data class FileMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "document_uri") val documentUri: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    /** Path inside the picked folder, for display only. */
    @ColumnInfo(name = "display_path") val displayPath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "extension") val extension: String,
)

@Entity(tableName = "locations")
data class LocationMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val provider: String,
    val timestamp: Long,
)

/** One row per scan. Only counts are kept here; the data itself lives in the tables above. */
@Entity(tableName = "scan_logs")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    @ColumnInfo(name = "apps_count") val appsCount: Int,
    @ColumnInfo(name = "media_count") val mediaCount: Int,
    @ColumnInfo(name = "files_count") val filesCount: Int,
    @ColumnInfo(name = "usage_count") val usageCount: Int,
    /** Human-readable notes about skipped or failed parts, one per line. */
    @ColumnInfo(name = "notes") val notes: String,
)

/**
 * A small record of one scan, used to show what changed since the previous scan.
 * [payload] is a serialized ScanSnapshot: app list with granted sensitive permissions and the
 * watcher signals. No photo, file or location data is kept here.
 */
@Entity(tableName = "scan_snapshots")
data class ScanSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    @ColumnInfo(name = "payload") val payload: String,
)

