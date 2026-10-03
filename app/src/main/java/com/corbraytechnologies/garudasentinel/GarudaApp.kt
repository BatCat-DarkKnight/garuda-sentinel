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

package com.corbraytechnologies.garudasentinel

import android.app.Application
import android.content.Context
import com.corbraytechnologies.garudasentinel.collect.AppCollector
import com.corbraytechnologies.garudasentinel.collect.DeviceCollector
import com.corbraytechnologies.garudasentinel.collect.FileCollector
import com.corbraytechnologies.garudasentinel.collect.LocationCollector
import com.corbraytechnologies.garudasentinel.collect.MediaCollector
import com.corbraytechnologies.garudasentinel.collect.UsageCollector
import com.corbraytechnologies.garudasentinel.collect.WatcherCollector
import com.corbraytechnologies.garudasentinel.data.AppDatabase
import com.corbraytechnologies.garudasentinel.data.SettingsStore
import com.corbraytechnologies.garudasentinel.data.StorageMode
import com.corbraytechnologies.garudasentinel.export.DataWiper
import com.corbraytechnologies.garudasentinel.export.ExportService
import com.corbraytechnologies.garudasentinel.scan.ScanCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GarudaApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * Hand-rolled dependency container. Everything holds the application context only,
 * so no Activity can leak through a singleton.
 */
class AppContainer(app: Context) {
    val context: Context = app.applicationContext

    /** Outlives screens, so a scan keeps running when the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True when the user chose "Forget results when I close the app": the database is then in memory only. */
    val memoryOnly: Boolean = StorageMode.isMemoryOnly(context)

    val db: AppDatabase = run {
        // Remove any results saved before the setting was turned on.
        if (memoryOnly) context.deleteDatabase(AppDatabase.NAME)
        AppDatabase.create(context, inMemory = memoryOnly)
    }
    val settings = SettingsStore(context)

    val deviceCollector = DeviceCollector(context)
    val locationCollector = LocationCollector(context)
    val fileCollector = FileCollector(context)
    val watcherCollector = WatcherCollector(context)

    val scanCoordinator = ScanCoordinator(
        scope = appScope,
        db = db,
        settings = settings,
        apps = AppCollector(context),
        media = MediaCollector(context),
        files = fileCollector,
        usage = UsageCollector(context),
        watchers = watcherCollector,
    )

    val appVersion: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "unknown"

    val exportService = ExportService(context, db, deviceCollector, scanCoordinator, appVersion)
    val dataWiper = DataWiper(context, db, settings)
}
