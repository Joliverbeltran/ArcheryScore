package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.repository.SessionRepository
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {

    suspend fun delete(sessionId: String, confirmed: Boolean = false) {
        check(confirmed) { "Deletion requires explicit confirmation" }
        repository.deleteSession(sessionId)
    }
}