package com.archeryscore.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.data.csv.CsvImporter
import com.archeryscore.app.data.csv.CsvImportResult
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val sessions: List<Session> = emptyList(),
)

sealed interface ImportMessage {
    data class Success(val imported: Int, val skipped: Int) : ImportMessage
    data class Failure(val row: Int, val column: String, val reason: String) : ImportMessage
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = sessionRepository.observeSessions()
        .map { list ->
            HistoryUiState(
                loading = false,
                sessions = list.sortedByDescending { it.date },
            )
        }
        .catch { emit(HistoryUiState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    private val _importMessage = MutableStateFlow<ImportMessage?>(null)
    val importMessage: StateFlow<ImportMessage?> = _importMessage.asStateFlow()

    fun importCsv(csvText: String) {
        viewModelScope.launch {
            _importMessage.value = when (val result = CsvImporter.import(csvText, Instant.now())) {
                is CsvImportResult.Failure -> {
                    val e = result.error
                    ImportMessage.Failure(row = e.row, column = e.column, reason = e.reason)
                }
                is CsvImportResult.Success -> {
                    val summary = sessionRepository.importSessions(result.sessions)
                    ImportMessage.Success(imported = summary.imported, skipped = summary.skipped)
                }
            }
        }
    }

    fun clearImportMessage() { _importMessage.value = null }
}