package com.archeryscore.app.data.sync

data class RemoteSession(
    val id: String,
    val userId: String,
    val date: Long,
    val roundType: String,
    val distanceM: Int,
    val discipline: String,
    val endCount: Int,
    val arrowsPerEnd: Int,
    val notes: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSyncedAt: Long?,
)

data class RemoteEndWithArrows(
    val id: String,
    val sessionId: String,
    val endNumber: Int,
    val arrows: List<RemoteArrow>,
)

data class RemoteArrow(
    val id: String,
    val endId: String,
    val arrowNumber: Int,
    val score: Int,
    val isXRing: Boolean,
    val editedAt: Long,
)

interface SessionRemoteDataSource {
    suspend fun pushSession(row: RemoteSession)
    suspend fun pushEnd(end: RemoteEndWithArrows)
    suspend fun pushArrow(arrow: RemoteArrow)
    suspend fun deleteSession(id: String)
}

object PayloadCodec {

    fun decodeSessionV1(payload: String): RemoteSession {
        val parts = payload.split("|")
        require(parts.size >= 12) { "Malformed session payload V1" }
        fun p(i: Int): String = parts.getOrElse(i) { "" }
        return RemoteSession(
            id = p(0),
            userId = p(1),
            date = p(2).toLongOrNull() ?: 0L,
            roundType = p(3),
            distanceM = p(4).toIntOrNull() ?: 0,
            discipline = p(5),
            endCount = p(6).toIntOrNull() ?: 0,
            arrowsPerEnd = p(7).toIntOrNull() ?: 0,
            notes = p(8).takeIf { it.isNotEmpty() },
            status = p(9),
            createdAt = p(10).toLongOrNull() ?: 0L,
            updatedAt = p(11).toLongOrNull() ?: 0L,
            lastSyncedAt = p(12).takeIf { it.isNotEmpty() }?.toLongOrNull(),
        )
    }

    fun decodeEndV1(payload: String): RemoteEndWithArrows {
        val parts = payload.split("|")
        require(parts.size >= 4 && (parts.size - 4) % 6 == 0) {
            "Malformed end payload V1"
        }
        val arrows = (4 until parts.size step 6).map { i ->
            RemoteArrow(
                id = parts[i],
                endId = parts[i + 1],
                arrowNumber = parts[i + 2].toInt(),
                score = parts[i + 3].toInt(),
                isXRing = parts[i + 4].toBoolean(),
                editedAt = parts[i + 5].toLong(),
            )
        }
        return RemoteEndWithArrows(
            id = parts[1],
            sessionId = parts[0],
            endNumber = parts[2].toInt(),
            arrows = arrows,
        )
    }

    fun decodeArrowV1(payload: String): RemoteArrow {
        val parts = payload.split("|")
        require(parts.size == 6) { "Malformed arrow payload V1" }
        return RemoteArrow(
            id = parts[0],
            endId = parts[1],
            arrowNumber = parts[2].toInt(),
            score = parts[3].toInt(),
            isXRing = parts[4].toBoolean(),
            editedAt = parts[5].toLong(),
        )
    }

    fun decodeDeletion(payload: String): String {
        val parts = payload.split("|")
        require(parts.size == 2 && parts[0] == "DELETE") { "Malformed deletion payload V1" }
        return parts[1]
    }
}