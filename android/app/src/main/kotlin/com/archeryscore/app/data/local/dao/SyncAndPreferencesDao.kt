package com.archeryscore.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.archeryscore.app.data.local.entity.PreferencesEntity
import com.archeryscore.app.data.local.entity.SyncWriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncWriteDao {

    @Insert
    suspend fun insert(write: SyncWriteEntity)

    @Query("SELECT * FROM sync_writes WHERE user_id = :userId ORDER BY created_at ASC LIMIT :limit")
    suspend fun pending(userId: String, limit: Int): List<SyncWriteEntity>

    @Query("SELECT * FROM sync_writes WHERE user_id = :userId ORDER BY created_at ASC")
    fun observePending(userId: String): Flow<List<SyncWriteEntity>>

    @Query("DELETE FROM sync_writes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sync_writes SET attempts = attempts + 1 WHERE id = :id")
    suspend fun bumpAttempts(id: Long)

    @Query("SELECT COUNT(*) FROM sync_writes WHERE user_id = :userId")
    suspend fun countForUser(userId: String): Int
}

@Dao
interface PreferencesDao {

    @Query("SELECT * FROM prefs WHERE user_id = :userId LIMIT 1")
    suspend fun get(userId: String): PreferencesEntity?

    @Insert
    suspend fun insert(prefs: PreferencesEntity)

    @Query("UPDATE prefs SET default_round_type = :roundType, default_end_count = :endCount, " +
        "default_arrows_per_end = :arrowsPerEnd, default_distance_m = :distanceM, " +
        "default_discipline = :discipline, count_x_rings_deeply = :countXRingsDeeply WHERE user_id = :userId")
    suspend fun update(
        userId: String,
        roundType: String,
        endCount: Int,
        arrowsPerEnd: Int,
        distanceM: Int,
        discipline: String,
        countXRingsDeeply: Boolean,
    )
}