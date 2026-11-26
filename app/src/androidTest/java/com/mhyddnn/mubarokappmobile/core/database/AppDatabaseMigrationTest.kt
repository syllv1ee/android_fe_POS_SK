package com.mhyddnn.mubarokappmobile.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrateVersion1ToVersion3_preservesExistingData() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO sync_queue
                (id, owner, kind, path, body, createdAt, status, error, method)
                VALUES
                ('queue-1', 'tester', 'plan-create', 'plans', '{}', 1000, 'PENDING', NULL, 'POST')
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO plan_cache (owner, id, position, json)
                VALUES ('tester', 10, 0, '{"planNo":"PLAN-TEST"}')
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO master_data_cache (owner, json)
                VALUES ('tester', '{"employees":[]}')
                """.trimIndent()
            )

            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
        )

        try {
            assertEquals(1L, countRows(db, "sync_queue"))
            assertEquals(1L, countRows(db, "plan_cache"))
            assertEquals(1L, countRows(db, "master_data_cache"))

            assertEquals(0L, countRows(db, "visit_cache"))
            assertEquals(0L, countRows(db, "visit_market_area_cache"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migrateVersion2ToVersion3_preservesVisitCache() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO visit_cache
                (owner, localId, serverId, idempotencyKey, position, json, syncStatus)
                VALUES
                ('tester', -1, NULL, 'visit-key-1', 0, '{}', 'PENDING')
                """.trimIndent()
            )

            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            MIGRATION_2_3,
        )

        try {
            assertEquals(1L, countRows(db, "visit_cache"))
            assertEquals(0L, countRows(db, "visit_market_area_cache"))

            db.query(
                """
                SELECT localId, idempotencyKey, syncStatus
                FROM visit_cache
                WHERE owner = 'tester'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(-1, cursor.getInt(0))
                assertEquals("visit-key-1", cursor.getString(1))
                assertEquals("PENDING", cursor.getString(2))
            }
        } finally {
            db.close()
        }
    }

    private fun countRows(
        db: SupportSQLiteDatabase,
        tableName: String,
    ): Long {
        return db.query("SELECT COUNT(*) FROM `$tableName`").use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                0L
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
