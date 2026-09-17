package com.archeryscore.app.ui.record

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.PlacementScorer
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.usecase.EditScoreUseCase
import com.archeryscore.app.domain.usecase.SessionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class PlacementPoint(
    val x: Float,
    val y: Float,
)

data class PlacementFlow(
    val arrow: Arrow,
    val pending: PlacementPoint? = null,
    val isCorrection: Boolean = false,
    val previousLabel: String? = null,
)

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

    private val _placement = MutableStateFlow<PlacementFlow?>(null)
    val placement: StateFlow<PlacementFlow?> = _placement

    private val confirmedPointsByArrowId = mutableMapOf<UUID, PlacementPoint>()
    private val confirmedByEndId = mutableMapOf<UUID, List<PlacementPoint>>()
    private val _confirmedMarkers = MutableStateFlow<List<PlacementPoint>>(emptyList())
    val confirmedMarkers: StateFlow<List<PlacementPoint>> = _confirmedMarkers

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
                    score = 0,
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

    fun startPlacement(arrow: Arrow) {
        val confirmed = arrow.score > 0 || arrow.isXRing
        _placement.value = PlacementFlow(
            arrow = arrow,
            isCorrection = confirmed,
            previousLabel = if (confirmed) {
                if (arrow.isXRing) "${arrow.score} · X" else "${arrow.score}"
            } else null,
        )
        updateConfirmedMarkersForEnd(arrow.endId)
    }

    fun updatePending(point: PlacementPoint) {
        _placement.update { it?.copy(pending = point) }
    }

    fun confirmPlacement() {
        val flow = _placement.value ?: return
        val point = flow.pending ?: return
        val current = detail.value ?: return
        val result = PlacementScorer.place(
            x = point.x,
            y = point.y,
            targetType = current.session.targetType,
            scoringType = current.session.roundType,
        )
        confirmedPointsByArrowId[flow.arrow.id] = point
        viewModelScope.launch {
            editScoreUseCase.edit(
                sessionId = sessionId,
                arrow = flow.arrow,
                newScore = result.score,
                isXRing = result.isXRing,
            )
            advanceToNextArrow(flow.arrow, current)
        }
    }

    fun discardPlacement() {
        _placement.value = null
    }

    private fun advanceToNextArrow(placed: Arrow, current: SessionDetail) {
        val end = current.ends.firstOrNull { end ->
            end.arrows.any { it.id == placed.id }
        }
        val nextArrow = end?.arrows
            ?.sortedBy { it.arrowNumber }
            ?.firstOrNull { it.arrowNumber > placed.arrowNumber }
        if (nextArrow == null) {
            discardPlacement()
        } else {
            startPlacement(nextArrow)
        }
    }

    private fun updateConfirmedMarkersForEnd(endId: UUID) {
        val current = detail.value ?: return
        val end = current.ends.firstOrNull { it.end.id == endId } ?: return
        val points = end.arrows
            .sortedBy { it.arrowNumber }
            .mapNotNull { arrow -> confirmedPointsByArrowId[arrow.id] }
        _confirmedMarkers.value = points
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