package com.archeryscore.app.test

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.ImportedSession
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.model.UserPreferences
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.ImportResult
import com.archeryscore.app.domain.repository.PreferencesRepository
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.repository.StatsRepository
import com.archeryscore.app.domain.repository.StatsSnapshot
import com.archeryscore.app.domain.usecase.DisciplineStats
import com.archeryscore.app.domain.usecase.StatsAggregate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class FakeSessionRepository : SessionRepository {

    private val sessions = mutableMapOf<String, Session>()
    private val detailFlow = MutableStateFlow<SessionDetail?>(null)
    private val listFlow = MutableStateFlow<List<Session>>(emptyList())

    override fun observeSessions(): Flow<List<Session>> = listFlow

    override fun observeSessionDetail(sessionId: String): Flow<SessionDetail?> = detailFlow

    override suspend fun getSession(sessionId: String): Session? = sessions[sessionId]

    override suspend fun getActiveSession(): Session? =
        sessions.values.firstOrNull { it.status == SessionStatus.ACTIVE }

    override fun observeActiveSession(): Flow<Session?> =
        listFlow.map { items -> items.firstOrNull { it.status == SessionStatus.ACTIVE } }

    override suspend fun createSession(session: Session): Session {
        sessions[session.id.toString()] = session
        refreshFlows()
        return session
    }

    override suspend fun completeSession(sessionId: String) {
        sessions[sessionId]?.let { s ->
            sessions[sessionId] = s.copy(status = SessionStatus.COMPLETE)
            refreshFlows()
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.remove(sessionId)
        refreshFlows()
    }

    override suspend fun resumeSessionDetail(sessionId: String): SessionDetail? = detailFlow.value

    override suspend fun saveEnd(session: Session, arrows: List<Arrow>, newEndNumber: Int): End {
        val end = End(
            id = arrows.firstOrNull()?.endId ?: UUID.randomUUID(),
            sessionId = session.id,
            endNumber = newEndNumber,
            createdAt = java.time.Instant.now(),
        )
        return end
    }

    override suspend fun updateArrow(
        session: Session,
        arrow: Arrow,
        newScore: Int,
        newIsXRing: Boolean,
    ): Arrow = arrow.copy(score = newScore, isXRing = newIsXRing)

    override suspend fun getTotals(sessionId: String): SessionTotals? = null

    override suspend fun importSessions(imported: List<ImportedSession>): ImportResult =
        ImportResult(imported = imported.size, skipped = 0)

    fun installDetail(detail: SessionDetail?) {
        detailFlow.value = detail
    }

    fun installedDetail(): SessionDetail? = detailFlow.value

    private fun refreshFlows() {
        listFlow.value = sessions.values.toList()
    }
}

class FakePreferencesRepository : PreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences())

    override fun observePreferences(): Flow<UserPreferences> = prefs

    override suspend fun updatePreferences(prefs: UserPreferences) {
        this.prefs.value = prefs
    }
}

class FakeStatsRepository : StatsRepository {
    private val stats = MutableStateFlow(StatsSnapshot(StatsAggregate(0, 0, 0, null, 0, 0), emptyList(), false))

    override fun observeStats(from: Instant?, to: Instant?): Flow<StatsSnapshot> = stats

    fun setSnapshot(snapshot: StatsSnapshot) {
        stats.value = snapshot
    }
}