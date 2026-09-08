package com.archeryscore.app.data.repository

import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.data.mapper.Mapper
import com.archeryscore.app.data.sync.SyncOutboxWriter
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.ScoreValidator
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionListItem
import com.archeryscore.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomSessionRepository(
    private val sessionDao: SessionDao,
    private val endDao: EndDao,
    private val arrowDao: ArrowDao,
    private val syncWriteDao: SyncWriteDao,
    private val outbox: SyncOutboxWriter,
) : SessionRepository {

    override fun observeSessions(userId: String): Flow<List<SessionListItem>> =
        combine(sessionDao.observeAll(userId), syncWriteDao.observePending(userId)) {
                sessions, pending ->
            val pendingIds = pending.map { it.entityId }.toSet()
            sessions.map { entity ->
                val session = Mapper.sessionFromEntity(entity)
                val status = if (session.id.toString() in pendingIds) SyncStatus.PENDING
                else SyncStatus.SYNCED
                SessionListItem(session, status)
            }
        }

    override fun observeSessionDetail(sessionId: String): Flow<SessionDetail?> =
        combine(
            sessionDao.observeById(sessionId),
            endDao.observeBySession(sessionId),
        ) { sessionEntity, ends ->
            val sessionEntityLocal = sessionEntity ?: return@combine null
            val endIds = ends.map { it.id }
            if (endIds.isEmpty()) {
                Materialize(sessionEntityLocal, emptyList(), emptyList())
            } else {
                val arrows = arrowDao.getForEnds(endIds)
                Materialize(sessionEntityLocal, ends, arrows)
            }
        }

    private fun Materialize(
        sessionEntity: com.archeryscore.app.data.local.entity.SessionEntity,
        ends: List<com.archeryscore.app.data.local.entity.EndEntity>,
        arrows: List<com.archeryscore.app.data.local.entity.ArrowEntity>,
    ): SessionDetail {
        val session = Mapper.sessionFromEntity(sessionEntity)
        val arrowsByEnd = arrows.groupBy { it.endId }
        val endsWith = ends.sortedBy { it.endNumber }.map { endEntity ->
            val end = Mapper.endFromEntity(endEntity)
            val endArrows = arrowsByEnd[endEntity.id].orEmpty().sortedBy { it.arrowNumber }
                .map(Mapper::arrowFromEntity)
            EndWithArrows(end, endArrows)
        }
        return SessionDetail(
            session = session,
            ends = endsWith,
            total = endsWith.sumOf { with -> with.arrows.sumOf { it.score } },
            xCount = endsWith.sumOf { with -> with.arrows.count { it.isXRing } },
        )
    }

    override suspend fun getSession(sessionId: String): Session? =
        sessionDao.getById(sessionId)?.let(Mapper::sessionFromEntity)

    override suspend fun getActiveSession(userId: String): Session? =
        sessionDao.getActive(userId)?.let(Mapper::sessionFromEntity)

    override suspend fun createSession(session: Session): Session {
        val entity = Mapper.sessionToEntity(session)
        sessionDao.upsert(entity)
        outbox.enqueue(
            userId = entity.userId,
            entityType = "session",
            entityId = entity.id,
            payload = PayloadSerializer.session(entity),
        )
        return session
    }

    override suspend fun completeSession(sessionId: String) {
        val now = Instant.now()
        sessionDao.updateStatus(sessionId, SessionStatus.COMPLETE.name, now.toEpochMilli())
        sessionDao.getById(sessionId)?.let { entity ->
            outbox.enqueue(
                userId = entity.userId,
                entityType = "session",
                entityId = entity.id,
                payload = PayloadSerializer.session(entity),
            )
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.deleteById(sessionId)
        outbox.enqueue(
            userId = entity.userId,
            entityType = "session",
            entityId = entity.id,
            payload = PayloadSerializer.deletion(entity.id),
            operation = "DELETE",
        )
    }

    override suspend fun resumeSessionDetail(sessionId: String): SessionDetail? =
        MaterializeFromStorage(sessionId)

    private suspend fun MaterializeFromStorage(sessionId: String): SessionDetail? {
        val sessionEntity = sessionDao.getById(sessionId) ?: return null
        val ends = endDao.getBySession(sessionId)
        val arrows = arrowDao.getForEnds(ends.map { it.id })
        return Materialize(sessionEntity, ends, arrows)
    }

    override suspend fun saveEnd(
        session: Session,
        arrows: List<Arrow>,
        newEndNumber: Int,
    ): End {
        val end = End(
            sessionId = session.id,
            endNumber = newEndNumber,
            createdAt = Instant.now(),
        )
        endDao.upsert(Mapper.endToEntity(end))
        arrowDao.upsertAll(arrows.map(Mapper::arrowToEntity))
        val now = Instant.now()
        val updated = session.copy(updatedAt = now)
        sessionDao.upsert(Mapper.sessionToEntity(updated))
        outbox.enqueue(
            userId = session.userId,
            entityType = "end",
            entityId = end.id.toString(),
            payload = PayloadSerializer.end(end, arrows),
        )
        return end
    }

    override suspend fun updateArrow(
        session: Session,
        arrow: Arrow,
        newScore: Int,
        newIsXRing: Boolean,
    ): Arrow {
        ScoreValidator.assertValid(newScore, session.roundType, newIsXRing)
        val editedAt = arrow.editedAt
        val updatedArrow = arrow.copy(
            score = newScore,
            isXRing = newIsXRing,
            editedAt = editedAt,
        )
        arrowDao.upsert(Mapper.arrowToEntity(updatedArrow))
        val now = Instant.now()
        sessionDao.upsert(Mapper.sessionToEntity(session.copy(updatedAt = now)))
        outbox.enqueue(
            userId = session.userId,
            entityType = "arrow",
            entityId = updatedArrow.id.toString(),
            payload = PayloadSerializer.arrow(updatedArrow),
        )
        return updatedArrow
    }

    override suspend fun getTotals(sessionId: String): SessionTotals? {
        val sessionEntity = sessionDao.getById(sessionId) ?: return null
        val ends = endDao.getBySession(sessionId)
        val arrows = arrowDao.getForEnds(ends.map { it.id })
        return SessionTotals(
            total = arrows.sumOf { it.score },
            xCount = arrows.count { it.isXRing },
            arrowsShot = arrows.size,
            endsShot = ends.size,
        )
    }
}

internal object PayloadSerializer {
    fun session(entity: com.archeryscore.app.data.local.entity.SessionEntity): String =
        // V1 compact JSON of remote-equivalent row.
        listOf(
            entity.id, entity.userId, entity.date.toString(), entity.roundType,
            entity.distanceM.toString(), entity.discipline, entity.endCount.toString(),
            entity.arrowsPerEnd.toString(), entity.notes.orEmpty(), entity.status,
            entity.createdAt.toString(), entity.updatedAt.toString(),
            entity.lastSyncedAt?.toString().orEmpty(),
        ).joinToString("|")

    fun end(
        end: End,
        arrows: List<Arrow>,
    ): String = buildString {
        append(end.sessionId).append('|').append(end.id).append('|').append(end.endNumber)
        for (arrow in arrows) {
            append('|').append(arrow.id).append('|').append(arrow.endId)
                .append('|').append(arrow.arrowNumber).append('|').append(arrow.score)
                .append('|').append(arrow.isXRing).append('|').append(arrow.editedAt.toEpochMilli())
        }
    }

    fun arrow(arrow: Arrow): String = listOf(
        arrow.id, arrow.endId, arrow.arrowNumber.toString(), arrow.score.toString(),
        arrow.isXRing.toString(), arrow.editedAt.toEpochMilli().toString(),
    ).joinToString("|")

    fun deletion(id: String): String = "DELETE|$id"
}