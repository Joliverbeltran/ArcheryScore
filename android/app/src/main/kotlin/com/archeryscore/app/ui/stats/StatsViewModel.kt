package com.archeryscore.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals
import com.archeryscore.app.domain.repository.AuthRepository
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.usecase.DisciplineStats
import com.archeryscore.app.domain.usecase.StatsAggregate
import com.archeryscore.app.domain.usecase.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val loading: Boolean = true,
    val aggregate: StatsAggregate = StatsAggregate(0, 0, 0, null, 0, 0),
    val byDiscipline: List<DisciplineStats> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = authRepository.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(StatsUiState(loading = false))
            } else {
                sessionRepository.observeSessions(userId).flatMapLatest { list ->
                    flow {
                        val sessions = list.map { it.session }
                        val totals = buildMap {
                            sessions.forEach { s ->
                                getTotals(s.id.toString())?.let { put(s.id.toString(), it) }
                            }
                        }
                        emit(
                            StatsUiState(
                                loading = false,
                                aggregate = StatsCalculator.aggregate(sessions, totals),
                                byDiscipline = StatsCalculator.byDiscipline(sessions, totals),
                            )
                        )
                    }
                }
            }
        }
        .catch { emit(StatsUiState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())

    private suspend fun getTotals(sessionId: String): SessionTotals? =
        sessionRepository.getTotals(sessionId)
}