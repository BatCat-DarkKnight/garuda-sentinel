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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY app_name COLLATE NOCASE")
    fun observeAll(): Flow<List<AppMetadataEntity>>

    @Query("SELECT * FROM apps")
    suspend fun getAll(): List<AppMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AppMetadataEntity>)

    @Query("DELETE FROM apps")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<AppMetadataEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY COALESCE(date_taken, last_modified) DESC")
    fun observeAll(): Flow<List<MediaMetadataEntity>>

    @Query("SELECT * FROM media")
    suspend fun getAll(): List<MediaMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaMetadataEntity>)

    @Query("DELETE FROM media")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<MediaMetadataEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files ORDER BY last_modified DESC")
    fun observeAll(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files")
    suspend fun getAll(): List<FileMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FileMetadataEntity>)

    @Query("DELETE FROM files")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<FileMetadataEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LocationMetadataEntity>>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    suspend fun getAll(): List<LocationMetadataEntity>

    @Insert
    suspend fun insert(item: LocationMetadataEntity)

    @Query("DELETE FROM locations")
    suspend fun clear()
}

@Dao
interface ScanLogDao {
    @Query("SELECT * FROM scan_logs ORDER BY started_at DESC")
    fun observeAll(): Flow<List<ScanLogEntity>>

    @Query("SELECT * FROM scan_logs ORDER BY started_at DESC")
    suspend fun getAll(): List<ScanLogEntity>

    @Insert
    suspend fun insert(item: ScanLogEntity): Long

    @Query("DELETE FROM scan_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scan_logs")
    suspend fun clear()
}

@Dao
interface ScanSnapshotDao {
    @Query("SELECT * FROM scan_snapshots ORDER BY taken_at DESC LIMIT :limit")
    fun observeLatest(limit: Int = 2): Flow<List<ScanSnapshotEntity>>

    @Query("SELECT * FROM scan_snapshots ORDER BY taken_at DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<ScanSnapshotEntity>

    @Insert
    suspend fun insert(item: ScanSnapshotEntity): Long

    @Query("DELETE FROM scan_snapshots WHERE id NOT IN (SELECT id FROM scan_snapshots ORDER BY taken_at DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM scan_snapshots")
    suspend fun clear()

    @Transaction
    suspend fun add(item: ScanSnapshotEntity, keep: Int = 20): Long {
        val id = insert(item)
        trimTo(keep)
        return id
    }
}

