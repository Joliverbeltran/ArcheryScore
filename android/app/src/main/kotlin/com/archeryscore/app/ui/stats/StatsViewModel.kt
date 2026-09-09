package com.archeryscore.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.domain.repository.AuthRepository
import com.archeryscore.app.domain.repository.StatsRepository
import com.archeryscore.app.domain.repository.StatsSnapshot
import com.archeryscore.app.domain.usecase.DisciplineStats
import com.archeryscore.app.domain.usecase.StatsAggregate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class StatsUiState(
    val loading: Boolean = true,
    val aggregate: StatsAggregate = StatsAggregate(0, 0, 0, null, 0, 0),
    val byDiscipline: List<DisciplineStats> = emptyList(),
    val needsMoreData: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val range = MutableStateFlow<Pair<Instant?, Instant?>?>(null)

    fun setRange(from: Instant?, to: Instant?) {
        range.value = from to to
    }

    val uiState: StateFlow<StatsUiState> = combine(authRepository.currentUserId, range) { userId, r ->
        userId to r
    }
        .flatMapLatest { (userId, r) ->
            if (userId == null) {
                flowOf(StatsUiState(loading = false))
            } else {
                statsRepository.observeStats(userId, r?.first, r?.second)
                    .map { snapshot: StatsSnapshot ->
                        StatsUiState(
                            loading = false,
                            aggregate = snapshot.aggregate,
                            byDiscipline = snapshot.byDiscipline,
                            needsMoreData = snapshot.needsMoreData,
                        )
                    }
            }
        }
        .catch { emit(StatsUiState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())
}
