package com.corbraytechnologies.garudasentinel.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks that a version 1 database from an earlier build opens on version 2 without losing data. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrates1To2AndKeepsScanResults() {
        helper.createDatabase(testDb, 1).use { db ->
            db.execSQL(
                "INSERT INTO apps (package_name, app_name, version_name, version_code, first_install_time, " +
                    "last_update_time, target_sdk, min_sdk, permissions, granted_permissions, is_system_app, " +
                    "category, installer) VALUES ('com.example.notes', 'Notes', '1.0', 1, 10, 20, 36, 29, " +
                    "'android.permission.CAMERA', 'android.permission.CAMERA', 0, 'Other', 'com.android.vending')"
            )
            db.execSQL(
                "INSERT INTO scan_logs (started_at, finished_at, apps_count, media_count, files_count, usage_count, notes) " +
                    "VALUES (1, 2, 1, 0, 0, 0, '')"
            )
        }

        val db = helper.runMigrationsAndValidate(testDb, 2, true, AppDatabase.MIGRATION_1_2)

        db.query("SELECT app_name FROM apps").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Notes", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM scan_logs").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        // The new table exists and is empty.
        db.query("SELECT COUNT(*) FROM scan_snapshots").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }
}
