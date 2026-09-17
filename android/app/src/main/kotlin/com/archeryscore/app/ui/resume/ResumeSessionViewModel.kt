package com.archeryscore.app.ui.resume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.TargetType
import com.archeryscore.app.domain.model.UserPreferences
import com.archeryscore.app.domain.repository.PreferencesRepository
import com.archeryscore.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ResumeUiState(
    val loading: Boolean = true,
    val activeSessionId: String? = null,
    val defaults: UserPreferences = UserPreferences(),
)

@HiltViewModel
class ResumeSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumeUiState())
    val uiState: StateFlow<ResumeUiState> = _uiState

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val prefs = preferencesRepository.observePreferences().first()
            _uiState.update {
                it.copy(
                    loading = false,
                    defaults = prefs,
                )
            }
            sessionRepository.observeActiveSession().collect { active ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        activeSessionId = active?.id?.toString(),
                    )
                }
            }
        }
    }

    fun createSession(
        roundType: RoundType,
        targetType: TargetType = TargetType.CM122,
        endCount: Int,
        arrowsPerEnd: Int,
        distanceM: Int,
        discipline: Discipline,
        notes: String? = null,
    ) {
        viewModelScope.launch {
            val now = Instant.now()
            val session = Session(
                date = now,
                roundType = roundType,
                targetType = targetType,
                distanceM = distanceM,
                discipline = discipline,
                endCount = endCount,
                arrowsPerEnd = arrowsPerEnd,
                notes = notes,
                status = SessionStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
            val created = sessionRepository.createSession(session)
            val updatedDefaults = _uiState.value.defaults.copy(
                defaultTargetType = targetType,
                defaultDistanceM = distanceM,
                defaultEndCount = endCount,
                defaultArrowsPerEnd = arrowsPerEnd,
            )
            preferencesRepository.updatePreferences(updatedDefaults)
            _uiState.update {
                it.copy(activeSessionId = created.id.toString(), defaults = updatedDefaults)
            }
        }
    }

    fun resumeSession(): String? = _uiState.value.activeSessionId
}