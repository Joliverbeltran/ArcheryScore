package com.archeryscore.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prefs")
data class PreferencesEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "default_round_type") val defaultRoundType: String,
    @ColumnInfo(name = "default_end_count") val defaultEndCount: Int,
    @ColumnInfo(name = "default_arrows_per_end") val defaultArrowsPerEnd: Int,
    @ColumnInfo(name = "default_distance_m") val defaultDistanceM: Int,
    @ColumnInfo(name = "default_discipline") val defaultDiscipline: String,
    @ColumnInfo(name = "count_x_rings_deeply") val countXRingsDeeply: Boolean,
)

enum class SyncOperation {
    UPSERT,
    DELETE,
}

@Entity(tableName = "sync_writes")
data class SyncWriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val operation: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val attempts: Int,
)