package com.archeryscore.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.data.local.entity.SessionEntity

private const val DB_NAME = "archery_score.db"
private const val TAG = "AppDatabase"

@Database(
    entities = [
        SessionEntity::class,
        EndEntity::class,
        ArrowEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun endDao(): EndDao
    abstract fun arrowDao(): ArrowDao

    companion object {

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .openHelperFactory(CorruptionSafeHelperFactory(context))
                .build()
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sessions_new` (
              `id` TEXT NOT NULL,
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
              PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "INSERT INTO `sessions_new` (`id`, `date`, `round_type`, `distance_m`, `discipline`, " +
                "`end_count`, `arrows_per_end`, `notes`, `status`, `created_at`, `updated_at`) " +
                "SELECT `id`, `date`, `round_type`, `distance_m`, `discipline`, `end_count`, " +
                "`arrows_per_end`, `notes`, `status`, `created_at`, `updated_at` FROM `sessions`",
        )
        db.execSQL("DROP TABLE `sessions`")
        db.execSQL("ALTER TABLE `sessions_new` RENAME TO `sessions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_date` ON `sessions` (`date`)")
        db.execSQL("DROP TABLE IF EXISTS `sync_writes`")
        db.execSQL("DROP TABLE IF EXISTS `prefs`")
    }
}

/**
 * FR-012 (corrupt-DB recovery): if SQLite reports a corrupt database, delete the
 * DB file (plus -wal/-shm) so the next open rebuilds an empty database instead
 * of crashing on every launch.
 */
private class CorruptionSafeHelperFactory(
    private val context: Context,
) : SupportSQLiteOpenHelper.Factory {

    private val delegate = FrameworkSQLiteOpenHelperFactory()

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val wrappedConfiguration = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(CorruptionSafeCallback(context, configuration.callback))
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(configuration.allowDataLossOnRecovery)
            .build()
        return delegate.create(wrappedConfiguration)
    }
}

private class CorruptionSafeCallback(
    private val context: Context,
    private val delegate: SupportSQLiteOpenHelper.Callback,
) : SupportSQLiteOpenHelper.Callback(delegate.version) {

    override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)

    override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
        delegate.onUpgrade(db, oldVersion, newVersion)

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
        delegate.onDowngrade(db, oldVersion, newVersion)

    override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)

    override fun onCorruption(db: SupportSQLiteDatabase) {
        Log.e(TAG, "Room database corrupted; deleting DB file and rebuilding empty DB.")
        delegate.onCorruption(db)
        db.close()
        context.deleteDatabase(DB_NAME)
    }
}