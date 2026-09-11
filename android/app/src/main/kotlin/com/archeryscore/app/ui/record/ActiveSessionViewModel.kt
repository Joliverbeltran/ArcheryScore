package com.archeryscore.app.ui.record

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.usecase.EditScoreUseCase
import com.archeryscore.app.domain.usecase.SessionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val editScoreUseCase: EditScoreUseCase,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val detail: StateFlow<SessionDetail?> = sessionRepository
        .observeSessionDetail(sessionId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun addEnd() {
        viewModelScope.launch {
            val current = detail.value ?: return@launch
            if (current.session.status == SessionStatus.COMPLETE) return@launch
            val nextEndNumber = current.ends.size + 1
            val endId = UUID.randomUUID()
            val now = Instant.now()
            val arrows = (1..current.session.arrowsPerEnd).map { n ->
                Arrow(
                    endId = endId,
                    arrowNumber = n,
                    score = current.session.roundType.maxScore,
                    editedAt = now,
                )
            }
            sessionRepository.saveEnd(
                session = current.session,
                arrowsPerEnd = arrows,
                newEndNumber = nextEndNumber,
            )
        }
    }

    fun recordArrow(arrow: Arrow, score: Int, isXRing: Boolean = false) {
        viewModelScope.launch {
            val current = detail.value ?: return@launch
            try {
                editScoreUseCase.edit(
                    sessionId = sessionId,
                    arrow = arrow,
                    newScore = score,
                    isXRing = isXRing,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record score for arrow ${arrow.id}", e)
            }
        }
    }

    companion object {
        private const val TAG = "ActiveSessionVM"
    }

    suspend fun confirmCompletion() {
        sessionRepository.completeSession(sessionId)
    }

    fun nextEndNumber(): Int = (detail.value?.ends?.size ?: 0) + 1

    fun isFullyScored(): Boolean {
        val d = detail.value ?: return false
        return SessionCalculator.isComplete(d.session, d.ends.flatMap { it.arrows })
    }
}