package com.archeryscore.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.domain.repository.AuthRepository
import com.archeryscore.app.domain.repository.SessionListItem
import com.archeryscore.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val sessions: List<SessionListItem> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = authRepository.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(HistoryUiState(loading = false))
            } else {
                sessionRepository.observeSessions(userId).map { list ->
                    HistoryUiState(
                        loading = false,
                        sessions = list.sortedByDescending { it.session.date },
                    )
                }
            }
        }
        .catch { emit(HistoryUiState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())
}