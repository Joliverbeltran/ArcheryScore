package com.archeryscore.app.data.repository

import android.util.Log
import com.archeryscore.app.BuildConfig
import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.data.mapper.Mapper
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.ImportedSession
import com.archeryscore.app.domain.model.ScoreValidator
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.ImportResult
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class RoomSessionRepository(
    private val sessionDao: SessionDao,
    private val endDao: EndDao,
    private val arrowDao: ArrowDao,
) : SessionRepository {

    override fun observeSessions(): Flow<List<Session>> =
        sessionDao.observeAll().map { sessions ->
            sessions.map(Mapper::sessionFromEntity)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSessionDetail(sessionId: String): Flow<SessionDetail?> {
        val endsFlow = endDao.observeBySession(sessionId)
        val arrowsFlow = endsFlow.flatMapLatest { ends ->
            if (ends.isEmpty()) {
                flowOf(emptyList())
            } else {
                arrowDao.observeForEnds(ends.map { it.id })
            }
        }
        return combine(
            sessionDao.observeById(sessionId),
            endsFlow,
            arrowsFlow,
        ) { sessionEntity, ends, arrows ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "observeSessionDetail[$sessionId] ends=${ends.size} " +
                    "arrows=${arrows.size} perEnd=${ends.map { end ->
                        val count = arrows.count { it.endId == end.id }
                        "end${end.endNumber}:$count"
                    }}")
            }
            val sessionEntityLocal = sessionEntity ?: return@combine null
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

    override suspend fun getActiveSession(): Session? =
        sessionDao.getActive()?.let(Mapper::sessionFromEntity)

    override fun observeActiveSession(): Flow<Session?> =
        sessionDao.observeAll().map { sessions ->
            sessions.firstOrNull { it.status == SessionStatus.ACTIVE.name }
                ?.let(Mapper::sessionFromEntity)
        }

    override suspend fun createSession(session: Session): Session {
        sessionDao.upsert(Mapper.sessionToEntity(session))
        return session
    }

    override suspend fun completeSession(sessionId: String) {
        val now = Instant.now()
        sessionDao.updateStatus(sessionId, SessionStatus.COMPLETE.name, now.toEpochMilli())
    }

    override suspend fun deleteSession(sessionId: String) {
        val endIds = endDao.getBySession(sessionId).map { it.id }
        if (endIds.isNotEmpty()) {
            arrowDao.deleteForEnds(endIds)
        }
        endDao.deleteForSession(sessionId)
        sessionDao.deleteById(sessionId)
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
            id = arrows.firstOrNull()?.endId ?: UUID.randomUUID(),
            sessionId = session.id,
            endNumber = newEndNumber,
            createdAt = Instant.now(),
        )
        endDao.upsert(Mapper.endToEntity(end))
        arrowDao.upsertAll(arrows.map(Mapper::arrowToEntity))
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "saveEnd end=$end arrowsToSave=${arrows.size} " +
                "withScores=${arrows.map { it.score }}")
        }
        val now = Instant.now()
        val updated = session.copy(updatedAt = now)
        sessionDao.upsert(Mapper.sessionToEntity(updated))
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

    override suspend fun importSessions(imported: List<ImportedSession>): ImportResult {
        var importedCount = 0
        var skippedCount = 0
        for (item in imported) {
            val sessionId = item.session.id.toString()
            if (sessionDao.getById(sessionId) != null) {
                skippedCount++
                continue
            }
            val now = Instant.now()
            val session = item.session.copy(updatedAt = now)
            sessionDao.upsert(Mapper.sessionToEntity(session))
            for (end in item.ends) {
                val endId = UUID.randomUUID()
                endDao.upsert(
                    EndEntity(
                        id = endId.toString(),
                        sessionId = sessionId,
                        endNumber = end.endNumber,
                        createdAt = session.date.toEpochMilli(),
                    ),
                )
                for (arrow in end.arrows) {
                    arrowDao.upsert(
                        ArrowEntity(
                            id = UUID.randomUUID().toString(),
                            endId = endId.toString(),
                            arrowNumber = arrow.arrowNumber,
                            score = arrow.score,
                            isXRing = arrow.isXRing,
                            editedAt = now.toEpochMilli(),
                        ),
                    )
                }
            }
            importedCount++
        }
        return ImportResult(imported = importedCount, skipped = skippedCount)
    }

    private companion object {
        const val TAG = "ArcheryScore"
    }
}