package com.archeryscore.app.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.data.csv.CsvArrowRow
import com.archeryscore.app.data.csv.CsvExporter
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.usecase.DeleteSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val detail: SessionDetail? = null,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
    private val deleteSessionUseCase: DeleteSessionUseCase,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val uiState: StateFlow<DetailUiState> = sessionRepository
        .observeSessionDetail(sessionId)
        .map { DetailUiState(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DetailUiState())

    fun deleteSession(confirmed: Boolean) {
        viewModelScope.launch {
            runCatching { deleteSessionUseCase.delete(sessionId, confirmed) }
        }
    }

    fun exportCsv(detail: SessionDetail): String {
        val rows = detail.ends.flatMap { e ->
            e.arrows.map { a ->
                CsvArrowRow(
                    sessionId = detail.session.id.toString(),
                    date = detail.session.date,
                    distanceM = detail.session.distanceM,
                    discipline = detail.session.discipline,
                    roundType = detail.session.roundType,
                    endNumber = e.end.endNumber,
                    arrowNumber = a.arrowNumber,
                    score = a.score,
                    isXRing = a.isXRing,
                )
            }
        }
        return CsvExporter.export(rows)
    }
}