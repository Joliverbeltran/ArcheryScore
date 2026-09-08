package com.archeryscore.app.data.mapper

import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.data.local.entity.PreferencesEntity
import com.archeryscore.app.data.local.entity.SessionEntity
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.model.UserPreferences
import java.time.Instant
import java.util.UUID

object Mapper {

    fun sessionToEntity(session: Session): SessionEntity = SessionEntity(
        id = session.id.toString(),
        userId = session.userId,
        date = session.date.toEpochMilli(),
        roundType = session.roundType.name,
        distanceM = session.distanceM,
        discipline = session.discipline.name,
        endCount = session.endCount,
        arrowsPerEnd = session.arrowsPerEnd,
        notes = session.notes,
        status = session.status.name,
        createdAt = session.createdAt.toEpochMilli(),
        updatedAt = session.updatedAt.toEpochMilli(),
        lastSyncedAt = session.lastSyncedAt?.toEpochMilli(),
    )

    fun sessionFromEntity(e: SessionEntity): Session = Session(
        id = UUID.fromString(e.id),
        userId = e.userId,
        date = Instant.ofEpochMilli(e.date),
        roundType = RoundType.valueOf(e.roundType),
        distanceM = e.distanceM,
        discipline = Discipline.valueOf(e.discipline),
        endCount = e.endCount,
        arrowsPerEnd = e.arrowsPerEnd,
        notes = e.notes,
        status = SessionStatus.valueOf(e.status),
        createdAt = Instant.ofEpochMilli(e.createdAt),
        updatedAt = Instant.ofEpochMilli(e.updatedAt),
        lastSyncedAt = e.lastSyncedAt?.let(Instant::ofEpochMilli),
    )

    fun endToEntity(end: End): EndEntity = EndEntity(
        id = end.id.toString(),
        sessionId = end.sessionId.toString(),
        endNumber = end.endNumber,
        createdAt = end.createdAt.toEpochMilli(),
    )

    fun endFromEntity(e: EndEntity): End = End(
        id = UUID.fromString(e.id),
        sessionId = UUID.fromString(e.sessionId),
        endNumber = e.endNumber,
        createdAt = Instant.ofEpochMilli(e.createdAt),
    )

    fun arrowToEntity(arrow: Arrow): ArrowEntity = ArrowEntity(
        id = arrow.id.toString(),
        endId = arrow.endId.toString(),
        arrowNumber = arrow.arrowNumber,
        score = arrow.score,
        isXRing = arrow.isXRing,
        editedAt = arrow.editedAt.toEpochMilli(),
    )

    fun arrowFromEntity(e: ArrowEntity): Arrow = Arrow(
        id = UUID.fromString(e.id),
        endId = UUID.fromString(e.endId),
        arrowNumber = e.arrowNumber,
        score = e.score,
        isXRing = e.isXRing,
        editedAt = Instant.ofEpochMilli(e.editedAt),
    )

    fun prefsToEntity(userId: String, prefs: UserPreferences): PreferencesEntity = PreferencesEntity(
        userId = userId,
        defaultRoundType = prefs.defaultRoundType.name,
        defaultEndCount = prefs.defaultEndCount,
        defaultArrowsPerEnd = prefs.defaultArrowsPerEnd,
        defaultDistanceM = prefs.defaultDistanceM,
        defaultDiscipline = prefs.defaultDiscipline.name,
        countXRingsDeeply = prefs.countXRingsDeeply,
    )

    fun prefsFromEntity(e: PreferencesEntity): UserPreferences = UserPreferences(
        defaultRoundType = RoundType.valueOf(e.defaultRoundType),
        defaultEndCount = e.defaultEndCount,
        defaultArrowsPerEnd = e.defaultArrowsPerEnd,
        defaultDistanceM = e.defaultDistanceM,
        defaultDiscipline = Discipline.valueOf(e.defaultDiscipline),
        countXRingsDeeply = e.countXRingsDeeply,
    )

    fun syncStatus(pendingWrites: Int): SyncStatus = when {
        pendingWrites > 0 -> SyncStatus.PENDING
        else -> SyncStatus.SYNCED
    }
}