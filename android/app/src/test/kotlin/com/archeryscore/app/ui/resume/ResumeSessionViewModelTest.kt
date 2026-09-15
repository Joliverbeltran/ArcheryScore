package com.archeryscore.app.ui.resume

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.test.FakeAuthRepository
import com.archeryscore.app.test.FakePreferencesRepository
import com.archeryscore.app.test.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeSessionViewModelTest {

    private val repo = FakeSessionRepository()
    private val prefs = FakePreferencesRepository()
    private val auth = FakeAuthRepository()
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resumes active session when one exists`() = runTest(dispatcher.scheduler) {
        val active = Session(
            userId = "test-user",
            date = java.time.Instant.now(),
            roundType = RoundType.TEN_ZONE,
            distanceM = 70,
            discipline = Discipline.OLYMPIC_RECURVE,
            endCount = 6,
            arrowsPerEnd = 3,
            status = SessionStatus.ACTIVE,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        repo.createSession(active)

        val vm = ResumeSessionViewModel(repo, prefs, auth)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.loading)
        assertEquals(active.id.toString(), state.activeSessionId)
    }

    @Test
    fun `no active session means null id and defaults from preferences`() = runTest(dispatcher.scheduler) {
        val vm = ResumeSessionViewModel(repo, prefs, auth)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.activeSessionId)
        assertEquals(RoundType.TEN_ZONE, state.defaults.defaultRoundType)
        assertEquals(18, state.defaults.defaultDistanceM)
    }

    @Test
    fun `creating a session exposes its id and marks active`() = runTest(dispatcher.scheduler) {
        val vm = ResumeSessionViewModel(repo, prefs, auth)
        advanceUntilIdle()

        vm.createSession(
            roundType = RoundType.FIVE_ZONE,
            endCount = 12,
            arrowsPerEnd = 6,
            distanceM = 30,
            discipline = Discipline.COMPOUND,
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.activeSessionId)
        val created = repo.getSession(state.activeSessionId!!)
        assertNotNull(created)
        assertEquals(SessionStatus.ACTIVE, created!!.status)
        assertEquals(RoundType.FIVE_ZONE, created.roundType)
        assertEquals(30, created.distanceM)
        assertEquals("test-user", created.userId)
    }

    @Test
    fun `session id from preferences flows into defaults`() = runTest(dispatcher.scheduler) {
        prefs.updatePreferences(
            "test-user",
            com.archeryscore.app.domain.model.UserPreferences(
                defaultRoundType = RoundType.FIVE_ZONE,
                defaultEndCount = 4,
                defaultArrowsPerEnd = 6,
                defaultDistanceM = 50,
                defaultDiscipline = Discipline.BAREBOW,
            ),
        )
        val vm = ResumeSessionViewModel(repo, prefs, auth)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(4, state.defaults.defaultEndCount)
        assertEquals(6, state.defaults.defaultArrowsPerEnd)
        assertEquals(50, state.defaults.defaultDistanceM)
        assertTrue(state.defaults.defaultDiscipline == Discipline.BAREBOW)
    }
}