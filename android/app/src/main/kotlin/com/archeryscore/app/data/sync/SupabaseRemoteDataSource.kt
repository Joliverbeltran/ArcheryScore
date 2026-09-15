package com.archeryscore.app.data.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import java.io.IOException

class SupabaseRemoteDataSource(
    private val client: SupabaseClient,
) : SessionRemoteDataSource {

    override suspend fun pushSession(row: RemoteSession) {
        client.postgrest.from("sessions").upsert(
            listOf(
                mapOf(
                    "id" to row.id,
                    "user_id" to row.userId,
                    "date" to row.date,
                    "round_type" to row.roundType,
                    "distance_m" to row.distanceM,
                    "discipline" to row.discipline,
                    "end_count" to row.endCount,
                    "arrows_per_end" to row.arrowsPerEnd,
                    "notes" to row.notes,
                    "status" to row.status,
                    "created_at" to row.createdAt,
                    "updated_at" to row.updatedAt,
                    "last_synced_at" to row.lastSyncedAt,
                )
            )
        )
    }

    override suspend fun pushEnd(end: RemoteEndWithArrows) {
        client.postgrest.from("ends").upsert(
            listOf(
                mapOf(
                    "id" to end.id,
                    "session_id" to end.sessionId,
                    "end_number" to end.endNumber,
                )
            )
        )
        client.postgrest.from("arrows").upsert(
            end.arrows.map { arrow ->
                mapOf(
                    "id" to arrow.id,
                    "end_id" to arrow.endId,
                    "arrow_number" to arrow.arrowNumber,
                    "score" to arrow.score,
                    "is_x_ring" to arrow.isXRing,
                    "edited_at" to arrow.editedAt,
                )
            }
        )
    }

    override suspend fun pushArrow(arrow: RemoteArrow) {
        client.postgrest.from("arrows").upsert(
            listOf(
                mapOf(
                    "id" to arrow.id,
                    "end_id" to arrow.endId,
                    "arrow_number" to arrow.arrowNumber,
                    "score" to arrow.score,
                    "is_x_ring" to arrow.isXRing,
                    "edited_at" to arrow.editedAt,
                )
            )
        )
    }

    override suspend fun deleteSession(id: String) {
        client.postgrest.from("sessions").delete {
            filter {
                eq("id", id)
            }
        }
    }
}

class DisabledRemoteDataSource : SessionRemoteDataSource {
    override suspend fun pushSession(row: RemoteSession) = throw IOException("Supabase not configured")
    override suspend fun pushEnd(end: RemoteEndWithArrows) = throw IOException("Supabase not configured")
    override suspend fun pushArrow(arrow: RemoteArrow) = throw IOException("Supabase not configured")
    override suspend fun deleteSession(id: String) = throw IOException("Supabase not configured")
}