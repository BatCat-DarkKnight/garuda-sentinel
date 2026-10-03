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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Local-only store for scan results. Schema history is exported to app/schemas;
 * any future schema change needs a real Migration, not destructive fallback.
 */
@Database(
    entities = [
        AppMetadataEntity::class,
        MediaMetadataEntity::class,
        FileMetadataEntity::class,
        LocationMetadataEntity::class,
        ScanLogEntity::class,
        ScanSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun mediaDao(): MediaDao
    abstract fun fileDao(): FileDao
    abstract fun locationDao(): LocationDao
    abstract fun scanLogDao(): ScanLogDao
    abstract fun scanSnapshotDao(): ScanSnapshotDao

    companion object {
        const val NAME = "garuda_sentinel.db"

        /**
         * Version 2 adds the per-scan snapshots behind "What changed since your last scan".
         * Scan results from version 1 are kept; only the new table is added.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scan_snapshots` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taken_at` INTEGER NOT NULL, " +
                        "`payload` TEXT NOT NULL)"
                )
            }
        }

        /** An on-disk database, or an in-memory one when the user chose not to keep scan results. */
        fun create(context: Context, inMemory: Boolean = false): AppDatabase =
            if (inMemory) {
                Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java).build()
            } else {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build()
            }
    }
}
