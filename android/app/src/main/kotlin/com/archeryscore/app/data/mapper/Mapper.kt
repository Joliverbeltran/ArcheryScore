package com.archeryscore.app.data.mapper

import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.data.local.entity.SessionEntity
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import java.time.Instant
import java.util.UUID

object Mapper {

    fun sessionToEntity(session: Session): SessionEntity = SessionEntity(
        id = session.id.toString(),
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
    )

    fun sessionFromEntity(e: SessionEntity): Session = Session(
        id = UUID.fromString(e.id),
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
}