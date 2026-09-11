package com.archeryscore.app.domain.repository

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.model.UserPreferences
import com.archeryscore.app.domain.usecase.DisciplineStats
import com.archeryscore.app.domain.usecase.StatsAggregate
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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

    fun observeActiveSession(userId: String): Flow<Session?>

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

interface StatsRepository {
    fun observeStats(userId: String, from: Instant?, to: Instant?): Flow<StatsSnapshot>
}

data class StatsSnapshot(
    val aggregate: StatsAggregate,
    val byDiscipline: List<DisciplineStats>,
    val needsMoreData: Boolean,
)

interface AuthRepository {
    val currentUserId: Flow<String?>
    suspend fun requireUserId(): String
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun restore(): AuthResult
    suspend fun signOut(): AuthResult
}

sealed class AuthResult {
    data object Success : AuthResult()
    data class Failure(val reason: AuthFailureReason) : AuthResult()
}

enum class AuthFailureReason {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    INVALID_CREDENTIALS,
    EMAIL_TAKEN,
    NETWORK,
    UNKNOWN,
}