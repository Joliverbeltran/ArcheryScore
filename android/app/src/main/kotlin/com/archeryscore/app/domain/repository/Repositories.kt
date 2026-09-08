package com.archeryscore.app.domain.repository

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

data class SessionListItem(
    val session: Session,
    val syncStatus: SyncStatus,
)

data class SessionDetail(
    val session: Session,
    val ends: List<EndWithArrows>,
    val total: Int,
    val xCount: Int,
)

data class EndWithArrows(
    val end: End,
    val arrows: List<Arrow>,
)

interface SessionRepository {

    fun observeSessions(userId: String): Flow<List<SessionListItem>>

    fun observeSessionDetail(sessionId: String): Flow<SessionDetail?>

    suspend fun getSession(sessionId: String): Session?

    suspend fun getActiveSession(userId: String): Session?

    suspend fun createSession(session: Session): Session

    suspend fun completeSession(sessionId: String)

    suspend fun deleteSession(sessionId: String)

    suspend fun resumeSessionDetail(sessionId: String): SessionDetail?

    suspend fun saveEnd(session: Session, arrowsPerEnd: List<Arrow>, newEndNumber: Int): End

    suspend fun updateArrow(
        session: Session,
        arrow: Arrow,
        newScore: Int,
        newIsXRing: Boolean,
    ): Arrow

    suspend fun getTotals(sessionId: String): SessionTotals?
}

interface PreferencesRepository {
    fun observePreferences(userId: String): Flow<UserPreferences>
    suspend fun updatePreferences(userId: String, prefs: UserPreferences)
}

interface SyncStatusRepository {
    fun observeStatus(userId: String): Flow<SyncStatus>
}

interface AuthRepository {
    val currentUserId: Flow<String?>
    suspend fun requireUserId(): String
}