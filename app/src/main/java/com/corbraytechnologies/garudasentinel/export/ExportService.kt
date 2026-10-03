package com.corbraytechnologies.garudasentinel.export

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.corbraytechnologies.garudasentinel.collect.DeviceCollector
import com.corbraytechnologies.garudasentinel.data.AppDatabase
import com.corbraytechnologies.garudasentinel.data.SettingsStore
import com.corbraytechnologies.garudasentinel.scan.ScanCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Writes exports only to a location the user picked in the system "Save as" dialog.
 * The app never writes scan data to shared storage on its own.
 */
class ExportService(
    private val context: Context,
    private val db: AppDatabase,
    private val device: DeviceCollector,
    private val scans: ScanCoordinator,
    private val appVersion: String,
) {

    /**
     * Writes the export and returns the number of bytes written. With a [password] the file is an
     * AES-256 encrypted ZIP holding one generic "garuda-export.json"; the password array is
     * cleared afterwards.
     */
    suspend fun export(
        target: Uri,
        include: Set<ExportCategory>,
        includePhotoLocations: Boolean = false,
        password: CharArray? = null,
    ): Int = withContext(Dispatchers.IO) {
        val usage = if (ExportCategory.APP_USAGE in include) {
            scans.usageSummary.value ?: scans.refreshUsage()
        } else {
            null
        }
        val sources = ExportSources(
            device = device.device(),
            battery = device.battery(),
            network = device.network(),
            input = device.input(),
            apps = db.appDao().getAll(),
            usage = usage,
            media = db.mediaDao().getAll(),
            files = db.fileDao().getAll(),
            locations = db.locationDao().getAll(),
            scanLogs = db.scanLogDao().getAll(),
        )
        val bytes = ExportBuilder.encode(
            ExportBuilder.build(sources, include, appVersion, System.currentTimeMillis(), includePhotoLocations)
        ).toByteArray()
        try {
            val stream = context.contentResolver.openOutputStream(target, "wt")
                ?: error("Could not open the chosen file for writing.")
            if (password == null) {
                stream.use { it.write(bytes) }
                bytes.size
            } else {
                val counting = CountingOutputStream(stream)
                EncryptedExport.write(counting, bytes, password)
                counting.count.toInt()
            }
        } finally {
            password?.fill('\u0000')
            bytes.fill(0)
        }
    }
}

/** Deletes every scan result the app holds and releases folder access. */
class DataWiper(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        // Release every folder grant the app still holds, not only the one on record, so a grant
        // left behind by an earlier folder change cannot outlive "Delete all".
        val resolver = context.contentResolver
        val uris = resolver.persistedUriPermissions.map { it.uri } + listOfNotNull(settings.filesTreeUri.first()?.toUri())
        uris.distinct().forEach { uri ->
            runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        settings.setFilesTreeUri(null)
    }
}

/** Counts the bytes written through it, for the "Export saved (size)" message. */
private class CountingOutputStream(private val out: java.io.OutputStream) : java.io.FilterOutputStream(out) {
    var count = 0L
        private set

    override fun write(b: Int) {
        out.write(b)
        count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        count += len
    }
}
