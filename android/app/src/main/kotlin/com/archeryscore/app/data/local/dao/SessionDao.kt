package com.archeryscore.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.archeryscore.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY date DESC")
    fun observeAll(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeById(id: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(userId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("UPDATE sessions SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM sessions WHERE user_id = :userId AND status = 'COMPLETE'")
    suspend fun countCompleted(userId: String): Int

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND status = 'COMPLETE' ORDER BY date")
    suspend fun allCompletedAsc(userId: String): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE user_id = :userId AND status = 'ACTIVE'")
    suspend fun countActive(userId: String): Int
}