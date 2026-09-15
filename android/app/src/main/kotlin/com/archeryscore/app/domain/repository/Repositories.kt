package com.archeryscore.app.domain.repository

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.ImportedSession
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.model.UserPreferences
import com.archeryscore.app.domain.usecase.DisciplineStats
import com.archeryscore.app.domain.usecase.StatsAggregate
import kotlinx.coroutines.flow.Flow
import java.time.Instant

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

    fun observeSessions(): Flow<List<Session>>

    fun observeSessionDetail(sessionId: String): Flow<SessionDetail?>

    suspend fun getSession(sessionId: String): Session?

    suspend fun getActiveSession(): Session?

    fun observeActiveSession(): Flow<Session?>

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

    suspend fun importSessions(imported: List<ImportedSession>): ImportResult
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
)

interface PreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun updatePreferences(prefs: UserPreferences)
}

interface StatsRepository {
    fun observeStats(from: Instant?, to: Instant?): Flow<StatsSnapshot>
}

data class StatsSnapshot(
    val aggregate: StatsAggregate,
    val byDiscipline: List<DisciplineStats>,
    val needsMoreData: Boolean,
)