package com.archeryscore.app.domain.model

import java.time.Instant
import java.util.UUID

data class Session(
    val id: UUID = UUID.randomUUID(),
    val date: Instant,
    val roundType: RoundType,
    val targetType: TargetType = TargetType.CM122,
    val distanceM: Int,
    val discipline: Discipline,
    val endCount: Int,
    val arrowsPerEnd: Int,
    val notes: String? = null,
    val status: SessionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class End(
    val id: UUID = UUID.randomUUID(),
    val sessionId: UUID,
    val endNumber: Int,
    val createdAt: Instant,
)

data class Arrow(
    val id: UUID = UUID.randomUUID(),
    val endId: UUID,
    val arrowNumber: Int,
    val score: Int,
    val isXRing: Boolean = false,
    val editedAt: Instant,
)