package com.corbraytechnologies.garudasentinel.export

import com.corbraytechnologies.garudasentinel.data.AppMetadataEntity
import com.corbraytechnologies.garudasentinel.data.FileMetadataEntity
import com.corbraytechnologies.garudasentinel.data.LocationMetadataEntity
import com.corbraytechnologies.garudasentinel.data.MediaMetadataEntity
import com.corbraytechnologies.garudasentinel.data.ScanLogEntity
import com.corbraytechnologies.garudasentinel.model.BatteryInfo
import com.corbraytechnologies.garudasentinel.model.DeviceInfo
import com.corbraytechnologies.garudasentinel.model.InputDetails
import com.corbraytechnologies.garudasentinel.model.NetworkDetails
import com.corbraytechnologies.garudasentinel.model.UsageSummary
import kotlinx.serialization.json.Json

/** Everything an export can contain, gathered by the caller. */
data class ExportSources(
    val device: DeviceInfo?,
    val battery: BatteryInfo?,
    val network: NetworkDetails?,
    val input: InputDetails?,
    val apps: List<AppMetadataEntity>,
    val usage: UsageSummary?,
    val media: List<MediaMetadataEntity>,
    val files: List<FileMetadataEntity>,
    val locations: List<LocationMetadataEntity>,
    val scanLogs: List<ScanLogEntity>,
)

/** Pure mapping from collected data to the export file. No Android or I/O dependencies. */
object ExportBuilder {

    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
    }

    fun build(
        sources: ExportSources,
        include: Set<ExportCategory>,
        appVersion: String,
        now: Long,
        includePhotoLocations: Boolean = false,
    ): ExportDocument {
        val notes = buildList {
            if (ExportCategory.APP_USAGE in include && sources.usage == null) {
                add("App usage not included: Usage access was not granted.")
            }
            if (ExportCategory.MEDIA in include && !includePhotoLocations) {
                add("Photo GPS left out: \"Include photo locations\" was not selected.")
            } else if (ExportCategory.MEDIA in include && sources.media.none { it.gpsLat != null }) {
                add("No photo GPS found. Either photos have no location, or photo location access was not granted.")
            }
            add("Categories such as app category and media source are estimates based on names and paths.")
        }
        fun has(c: ExportCategory) = c in include
        return ExportDocument(
            generatedAt = now,
            appVersion = appVersion,
            includedCategories = ExportCategory.entries.filter { has(it) }.map { it.name },
            excludedCategories = ExportCategory.entries.filterNot { has(it) }.map { it.name },
            notes = notes,
            photoLocationsIncluded = has(ExportCategory.MEDIA) && includePhotoLocations,
            device = sources.device.takeIf { has(ExportCategory.DEVICE) },
            battery = sources.battery.takeIf { has(ExportCategory.DEVICE) },
            network = sources.network.takeIf { has(ExportCategory.DEVICE) },
            input = sources.input.takeIf { has(ExportCategory.DEVICE) },
            apps = if (has(ExportCategory.APPS)) sources.apps.map { it.toExport() } else null,
            appUsage = if (has(ExportCategory.APP_USAGE)) sources.usage?.apps else null,
            screenOnTodayMs = if (has(ExportCategory.APP_USAGE)) sources.usage?.screenOnTodayMs else null,
            media = if (has(ExportCategory.MEDIA)) {
                sources.media.map { it.toExport().let { m -> if (includePhotoLocations) m else m.withoutGps() } }
            } else {
                null
            },
            files = if (has(ExportCategory.FILES)) sources.files.map { it.toExport() } else null,
            locations = if (has(ExportCategory.LOCATION)) sources.locations.map { it.toExport() } else null,
            scanHistory = if (has(ExportCategory.SCAN_HISTORY)) sources.scanLogs.map { it.toExport() } else null,
        )
    }

    fun encode(document: ExportDocument): String =
        json.encodeToString(ExportDocument.serializer(), document)
}

/** The same media entry with photo GPS removed; EXIF that only held GPS is dropped entirely. */
internal fun MediaExport.withoutGps(): MediaExport {
    val stripped = exif?.copy(gpsLatitude = null, gpsLongitude = null)
    val empty = stripped != null && listOf(stripped.cameraMake, stripped.cameraModel, stripped.orientation, stripped.iso, stripped.exposureTime, stripped.flash).all { it == null }
    return copy(exif = if (empty) null else stripped)
}
