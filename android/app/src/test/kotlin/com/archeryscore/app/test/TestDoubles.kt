package com.archeryscore.app.test

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.model.UserPreferences
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.PreferencesRepository
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionListItem
import com.archeryscore.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository : SessionRepository {

    private val sessions = mutableMapOf<String, Session>()
    private val detailFlow = MutableStateFlow<SessionDetail?>(null)
    private val listFlow = MutableStateFlow<List<SessionListItem>>(emptyList())

    override fun observeSessions(userId: String): Flow<List<SessionListItem>> = listFlow

    override fun observeSessionDetail(sessionId: String): Flow<SessionDetail?> = detailFlow

    override suspend fun getSession(sessionId: String): Session? = sessions[sessionId]

    override suspend fun getActiveSession(userId: String): Session? =
        sessions.values.firstOrNull { it.userId == userId && it.status == SessionStatus.ACTIVE }

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

    fun installDetail(detail: SessionDetail?) {
        detailFlow.value = detail
    }

    fun installedDetail(): SessionDetail? = detailFlow.value

    private fun refreshFlows() {
        val items = sessions.values.map { SessionListItem(it, SyncStatus.PENDING) }
        listFlow.value = items
    }
}

class FakeAuthRepository(
    var userId: String? = "test-user",
) : com.archeryscore.app.domain.repository.AuthRepository {
    private val _currentUserId = MutableStateFlow(userId)
    override val currentUserId: Flow<String?> = _currentUserId

    override suspend fun requireUserId(): String = checkNotNull(userId)
}

class FakePreferencesRepository : PreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences())

    override fun observePreferences(userId: String): Flow<UserPreferences> = prefs

    override suspend fun updatePreferences(userId: String, prefs: UserPreferences) {
        this.prefs.value = prefs
    }
}