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
