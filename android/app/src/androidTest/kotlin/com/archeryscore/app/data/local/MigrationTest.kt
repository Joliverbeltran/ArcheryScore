package com.archeryscore.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.data.local.dao.SessionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @Test
    fun migration1to2_preservesSessionsEndsArrows_dropsSyncAndPrefs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-test.db"
        val identityDb = "$dbName-identity"
        context.deleteDatabase(dbName)
        context.deleteDatabase(identityDb)

        val expectedIdentity = expectedIdentityHash(context, identityDb)
        createV1Database(context, dbName, expectedIdentity)

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .build()

        try {
            runBlocking {
                val dao: SessionDao = db.sessionDao()
                val session = dao.getById(SESSION_ID)
                assertNotNull("migrated session must be preserved", session)
                assertEquals(27, session!!.endCount * 9)
                assertEquals(DATE_MILLIS, session.date)
                assertEquals("TEN_ZONE", session.roundType)
                assertEquals("COMPLETE", session.status)
                assertEquals(UPDATED_AT_MILLIS, session.updatedAt)

                val ends = db.endDao().getBySession(SESSION_ID)
                assertEquals(1, ends.size)
                val arrows = db.arrowDao().getForEnds(ends.map { it.id })
                assertEquals(3, arrows.size)
            }

            val writable: SupportSQLiteDatabase = db.openHelper.writableDatabase
            val sessionColumns = writable.query("SELECT * FROM sessions LIMIT 0").columnNames.toSet()
            assertFalse("user_id dropped", sessionColumns.contains("user_id"))
            assertFalse("last_synced_at dropped", sessionColumns.contains("last_synced_at"))

            val tables = writable.query("SELECT name FROM sqlite_master WHERE type='table'")
                .use { c ->
                    buildSet {
                        while (c.moveToNext()) add(c.getString(0))
                    }
                }
            assertTrue(tables.none { it == "sync_writes" })
            assertTrue(tables.none { it == "prefs" })
        } finally {
            db.close()
            context.deleteDatabase(dbName)
            context.deleteDatabase(identityDb)
        }
    }

    private fun expectedIdentityHash(context: Context, dbName: String): String {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        return try {
            db.openHelper.readableDatabase
                .query("SELECT identity_hash FROM room_master_table WHERE id = 1")
                .use { c -> if (c.moveToFirst()) c.getString(0) else "" }
        } finally {
            db.close()
        }
    }

    private fun createV1Database(context: Context, dbName: String, identityHash: String) {
        val v1 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        v1.version = 1
        v1.execSQL(
            "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
        )
        v1.execSQL(
            "INSERT INTO room_master_table (id, identity_hash) VALUES (1, ?)",
            arrayOf(identityHash),
        )
        v1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sessions` (
              `id` TEXT NOT NULL,
              `user_id` TEXT NOT NULL,
              `date` INTEGER NOT NULL,
              `round_type` TEXT NOT NULL,
              `distance_m` INTEGER NOT NULL,
              `discipline` TEXT NOT NULL,
              `end_count` INTEGER NOT NULL,
              `arrows_per_end` INTEGER NOT NULL,
              `notes` TEXT,
              `status` TEXT NOT NULL,
              `created_at` INTEGER NOT NULL,
              `updated_at` INTEGER NOT NULL,
              `last_synced_at` INTEGER,
              PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_user_id` ON `sessions` (`user_id`)")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_date` ON `sessions` (`date`)")

        v1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ends` (
              `id` TEXT NOT NULL,
              `session_id` TEXT NOT NULL,
              `end_number` INTEGER NOT NULL,
              `created_at` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_ends_session_id_end_number` ON `ends` (`session_id`, `end_number`)")
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_ends_session_id` ON `ends` (`session_id`)")

        v1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `arrows` (
              `id` TEXT NOT NULL,
              `end_id` TEXT NOT NULL,
              `arrow_number` INTEGER NOT NULL,
              `score` INTEGER NOT NULL,
              `is_x_ring` INTEGER NOT NULL,
              `edited_at` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        v1.execSQL("CREATE INDEX IF NOT EXISTS `index_arrows_end_id` ON `arrows` (`end_id`)")

        v1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prefs` (
              `user_id` TEXT NOT NULL,
              `default_round_type` TEXT NOT NULL,
              `default_end_count` INTEGER NOT NULL,
              `default_arrows_per_end` INTEGER NOT NULL,
              `default_distance_m` INTEGER NOT NULL,
              `default_discipline` TEXT NOT NULL,
              `count_x_rings_deeply` INTEGER NOT NULL,
              PRIMARY KEY(`user_id`)
            )
            """.trimIndent(),
        )

        v1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_writes` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `user_id` TEXT NOT NULL,
              `entity_type` TEXT NOT NULL,
              `entity_id` TEXT NOT NULL,
              `operation` TEXT NOT NULL,
              `payload` TEXT NOT NULL,
              `created_at` INTEGER NOT NULL,
              `attempts` INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        v1.execSQL(
            "INSERT INTO sessions (id, user_id, date, round_type, distance_m, discipline, end_count, " +
                "arrows_per_end, notes, status, created_at, updated_at, last_synced_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                SESSION_ID,
                "user-1",
                DATE_MILLIS,
                "TEN_ZONE",
                18,
                "OLYMPIC_RECURVE",
                3,
                3,
                null,
                "COMPLETE",
                DATE_MILLIS,
                UPDATED_AT_MILLIS,
                DATE_MILLIS,
            ),
        )
        v1.execSQL(
            "INSERT INTO ends (id, session_id, end_number, created_at) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(END_ID, SESSION_ID, 1, DATE_MILLIS),
        )
        (1..3).forEach { n ->
            v1.execSQL(
                "INSERT INTO arrows (id, end_id, arrow_number, score, is_x_ring, edited_at) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("arrow-$n", END_ID, n, 9, 0, DATE_MILLIS),
            )
        }
        v1.execSQL(
            "INSERT INTO prefs (user_id, default_round_type, default_end_count, default_arrows_per_end, " +
                "default_distance_m, default_discipline, count_x_rings_deeply) " +
                "VALUES ('user-1', 'TEN_ZONE', 3, 3, 18, 'OLYMPIC_RECURVE', 0)",
        )
        v1.execSQL(
            "INSERT INTO sync_writes (user_id, entity_type, entity_id, operation, payload, created_at, attempts) " +
                "VALUES ('user-1', 'session', '$SESSION_ID', 'UPSERT', 'x', $DATE_MILLIS, 0)",
        )
        v1.close()
    }

    private companion object {
        const val SESSION_ID = "00000000-0000-0000-0000-000000000001"
        const val END_ID = "00000000-0000-0000-0000-000000000002"
        const val DATE_MILLIS = 1_728_582_300_000L
        const val UPDATED_AT_MILLIS = 1_728_582_400_000L
    }
}