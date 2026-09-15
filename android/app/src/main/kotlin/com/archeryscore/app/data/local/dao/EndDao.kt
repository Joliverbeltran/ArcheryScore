package com.archeryscore.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EndDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(end: EndEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(ends: List<EndEntity>)

    @Query("SELECT * FROM ends WHERE session_id = :sessionId ORDER BY end_number")
    fun observeBySession(sessionId: String): Flow<List<EndEntity>>

    @Query("SELECT * FROM ends WHERE session_id = :sessionId ORDER BY end_number")
    suspend fun getBySession(sessionId: String): List<EndEntity>

    @Query("SELECT * FROM ends WHERE id = :id")
    suspend fun getById(id: String): EndEntity?

    @Query("SELECT MAX(end_number) FROM ends WHERE session_id = :sessionId")
    suspend fun maxEndNumber(sessionId: String): Int?

    @Query("DELETE FROM ends WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}

@Dao
interface ArrowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(arrow: ArrowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(arrows: List<ArrowEntity>)

    @Query("SELECT * FROM arrows WHERE end_id IN (:endIds) ORDER BY arrow_number")
    suspend fun getForEnds(endIds: List<String>): List<ArrowEntity>

    @Query("SELECT * FROM arrows WHERE end_id IN (:endIds) ORDER BY arrow_number")
    fun observeForEnds(endIds: List<String>): Flow<List<ArrowEntity>>

    @Query("SELECT * FROM arrows WHERE end_id = :endId ORDER BY arrow_number")
    suspend fun getForEnd(endId: String): List<ArrowEntity>

    @Query("SELECT * FROM arrows WHERE end_id = :endId ORDER BY arrow_number")
    fun observeForEnd(endId: String): Flow<List<ArrowEntity>>

    @Query("DELETE FROM arrows WHERE end_id IN (:endIds)")
    suspend fun deleteForEnds(endIds: List<String>)
}