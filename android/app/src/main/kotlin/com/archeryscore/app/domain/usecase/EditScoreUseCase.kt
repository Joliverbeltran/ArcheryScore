package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.ScoreValidator
import com.archeryscore.app.domain.repository.SessionRepository
import javax.inject.Inject

class EditScoreUseCase @Inject constructor(
    private val repository: SessionRepository,
) {

    /**
     * Edits an arrow score. The direct (local) edit always wins locally; LWW
     * conflict resolution is applied when the outbox merge reaches the server.
     */
    suspend fun edit(
        sessionId: String,
        arrow: Arrow,
        newScore: Int,
        isXRing: Boolean = ScoreValidator.canBeXRing(newScore, RoundType.TEN_ZONE),
    ): Arrow {
        val session = repository.getSession(sessionId)
            ?: throw IllegalArgumentException("Session $sessionId not found")
        ScoreValidator.assertValid(newScore, session.roundType, isXRing)
        return repository.updateArrow(session, arrow, newScore, isXRing)
    }
}