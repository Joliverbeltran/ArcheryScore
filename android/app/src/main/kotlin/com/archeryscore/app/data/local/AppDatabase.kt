package com.archeryscore.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.PreferencesDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.data.local.entity.PreferencesEntity
import com.archeryscore.app.data.local.entity.SessionEntity
import com.archeryscore.app.data.local.entity.SyncWriteEntity

@Database(
    entities = [
        SessionEntity::class,
        EndEntity::class,
        ArrowEntity::class,
        PreferencesEntity::class,
        SyncWriteEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun endDao(): EndDao
    abstract fun arrowDao(): ArrowDao
    abstract fun syncWriteDao(): SyncWriteDao
    abstract fun preferencesDao(): PreferencesDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "archery_score.db")
                .build()
    }
}