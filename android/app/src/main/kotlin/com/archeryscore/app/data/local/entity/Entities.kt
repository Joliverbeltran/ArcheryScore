package com.archeryscore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [Index("date")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val date: Long,
    @ColumnInfo(name = "round_type") val roundType: String,
    @ColumnInfo(name = "target_type") val targetType: String = "CM122",
    @ColumnInfo(name = "distance_m") val distanceM: Int,
    val discipline: String,
    @ColumnInfo(name = "end_count") val endCount: Int,
    @ColumnInfo(name = "arrows_per_end") val arrowsPerEnd: Int,
    val notes: String?,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "ends",
    indices = [Index("session_id"), Index(value = ["session_id", "end_number"], unique = true)],
)
data class EndEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "end_number") val endNumber: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "arrows",
    indices = [Index("end_id")],
)
data class ArrowEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "end_id") val endId: String,
    @ColumnInfo(name = "arrow_number") val arrowNumber: Int,
    val score: Int,
    @ColumnInfo(name = "is_x_ring") val isXRing: Boolean,
    @ColumnInfo(name = "edited_at") val editedAt: Long,
)