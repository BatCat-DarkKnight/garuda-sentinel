package com.corbraytechnologies.garudasentinel.export

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.data.FileMetadataEntity
import com.corbraytechnologies.garudasentinel.data.LocationMetadataEntity
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.model.AppUsageInfo
import com.corbraytechnologies.garudasentinel.model.BatteryInfo
import com.corbraytechnologies.garudasentinel.model.DeviceInfo
import com.corbraytechnologies.garudasentinel.model.InputDetails
import com.corbraytechnologies.garudasentinel.model.NetworkDetails
import kotlinx.serialization.Serializable

/** Categories the user can include in or leave out of an export. */
enum class ExportCategory(val label: String, val onByDefault: Boolean = true) {
    DEVICE("Device, battery, network and keyboard"),
    APPS("Installed apps"),
    APP_USAGE("App usage (last 7 days)"),
    MEDIA("Photos, videos and audio"),
    FILES("Files in your chosen folder"),
    LOCATION("Location readings", onByDefault = false),
    SCAN_HISTORY("Scan history"),
}

/**
 * The export file format. Bump [SCHEMA_VERSION] whenever a field is renamed or removed.
 * Categories left out by the user are listed in [excludedCategories] and their fields are null.
 */
@Serializable
data class ExportDocument(
    val schemaVersion: Int = SCHEMA_VERSION,
    val generatedAt: Long,
    val appVersion: String,
    val includedCategories: List<String>,
    val excludedCategories: List<String>,
    val notes: List<String>,
    /** False unless the user ticked "Include photo locations"; GPS fields are then null. */
    val photoLocationsIncluded: Boolean = false,
    val device: DeviceInfo? = null,
    val battery: BatteryInfo? = null,
    val network: NetworkDetails? = null,
    val input: InputDetails? = null,
    val apps: List<AppExport>? = null,
    val appUsage: List<AppUsageInfo>? = null,
    val screenOnTodayMs: Long? = null,
    val media: List<MediaExport>? = null,
    val files: List<FileExport>? = null,
    val locations: List<LocationExport>? = null,
    val scanHistory: List<ScanLogExport>? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 2
    }
}

@Serializable
data class AppExport(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val isSystemApp: Boolean,
    val estimatedCategory: String,
    val installer: String?,
    val requestedPermissions: List<String>,
    val grantedPermissions: List<String>,
)

@Serializable
data class ExifExport(
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val cameraMake: String?,
    val cameraModel: String?,
    val orientation: Int?,
    val iso: Int?,
    val exposureTime: String?,
    val flash: String?,
)

@Serializable
data class MediaExport(
    val fileName: String,
    val folder: String,
    val type: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val dateTaken: Long?,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val estimatedSource: String,
    val exif: ExifExport?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val year: Int?,
)

@Serializable
data class FileExport(
    val fileName: String,
    val path: String,
    val mimeType: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

@Serializable
data class LocationExport(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val timestamp: Long,
)

@Serializable
data class ScanLogExport(
    val startedAt: Long,
    val finishedAt: Long,
    val appsCount: Int,
    val mediaCount: Int,
    val filesCount: Int,
    val usageCount: Int,
    val notes: List<String>,
)

fun AppMetadataEntity.toExport() = AppExport(
    packageName = packageName,
    appName = appName,
    versionName = versionName,
    versionCode = versionCode,
    firstInstallTime = firstInstallTime,
    lastUpdateTime = lastUpdateTime,
    targetSdk = targetSdkVersion,
    minSdk = minSdkVersion,
    isSystemApp = isSystemApp,
    estimatedCategory = appCategory,
    installer = installer,
    requestedPermissions = permissionList,
    grantedPermissions = grantedPermissionList,
)

fun MediaMetadataEntity.toExport(): MediaExport {
    val hasExif = listOf(gpsLat, gpsLong, cameraMake, cameraModel, orientation, iso, exposureTime, flash)
        .any { it != null }
    return MediaExport(
        fileName = fileName,
        folder = relativePath,
        type = type,
        mimeType = mimeType,
        sizeBytes = fileSize,
        lastModified = lastModified,
        dateTaken = dateTaken,
        width = width,
        height = height,
        durationMs = duration,
        estimatedSource = mediaCategory,
        exif = if (hasExif) ExifExport(gpsLat, gpsLong, cameraMake, cameraModel, orientation, iso, exposureTime, flash) else null,
        artist = artist,
        album = album,
        genre = genre,
        year = year,
    )
}

fun FileMetadataEntity.toExport() = FileExport(
    fileName = fileName,
    path = displayPath,
    mimeType = mimeType,
    extension = extension,
    sizeBytes = fileSize,
    lastModified = lastModified,
)

fun LocationMetadataEntity.toExport() = LocationExport(latitude, longitude, accuracy, provider, timestamp)

fun ScanLogEntity.toExport() = ScanLogExport(
    startedAt = startedAt,
    finishedAt = finishedAt,
    appsCount = appsCount,
    mediaCount = mediaCount,
    filesCount = filesCount,
    usageCount = usageCount,
    notes = notes.lines().filter { it.isNotBlank() },
)
