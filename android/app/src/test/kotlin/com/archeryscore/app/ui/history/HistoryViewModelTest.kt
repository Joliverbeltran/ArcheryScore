package com.archeryscore.app.ui.history

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.repository.SessionListItem
import com.archeryscore.app.test.FakeAuthRepository
import com.archeryscore.app.test.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeSessionRepository()
    private val auth = FakeAuthRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun session(offsetDays: Long) = Session(
        id = UUID.randomUUID(),
        userId = "u1",
        date = Instant.EPOCH.plusSeconds(offsetDays * 86400),
        roundType = RoundType.TEN_ZONE,
        distanceM = 18,
        discipline = Discipline.BAREBOW,
        endCount = 6,
        arrowsPerEnd = 3,
        status = SessionStatus.COMPLETE,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `sessions are ordered newest first`() = runTest(dispatcher.scheduler) {
        val older = session(1)
        val newer = session(2)
        runTestFactor(repo, older, newer)

        val vm = HistoryViewModel(repo, auth)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.sessions.size)
        assertEquals(newer.id, state.sessions[0].session.id)
        assertEquals(older.id, state.sessions[1].session.id)
    }

    @Test
    fun `sync status is surfaced on each item`() = runTest(dispatcher.scheduler) {
        val s = session(1)
        runTestFactor(repo, s)
        val vm = HistoryViewModel(repo, auth)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(SyncStatus.PENDING, vm.uiState.value.sessions.first().syncStatus)
    }

    @Test
    fun `unknown state equals null userId yields empty list`() = runTest(dispatcher.scheduler) {
        val noAuth = FakeAuthRepository(userId = null)
        val vm = HistoryViewModel(repo, noAuth)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.uiState.value.sessions.size)
    }

    @Test
    fun `detail view model exposes detail and exports csv rows`() = runTest(dispatcher.scheduler) {
        val s = session(1)
        val arrow = com.archeryscore.app.domain.model.Arrow(
            endId = UUID.randomUUID(),
            arrowNumber = 1,
            score = 10,
            isXRing = true,
            editedAt = Instant.EPOCH,
        )
        val end = com.archeryscore.app.domain.model.End(
            sessionId = s.id,
            endNumber = 1,
            createdAt = Instant.EPOCH,
        )
        repo.installDetail(SessionDetail(s, listOf(EndWithArrows(end, listOf(arrow))), total = 10, xCount = 1))

        val vm = SessionDetailViewModel(
            androidx.lifecycle.SavedStateHandle(mapOf("sessionId" to s.id.toString())),
            repo,
            com.archeryscore.app.domain.usecase.DeleteSessionUseCase(repo),
        )
        dispatcher.scheduler.advanceUntilIdle()

        val exported = vm.exportCsv(vm.uiState.value.detail!!)
        assertTrue(exported.startsWith("session_id,"))
        assertTrue(exported.contains(",10,true"))
        assertNotNull(vm.sessionId)
    }

    private suspend fun runTestFactor(repo: FakeSessionRepository, vararg sessions: Session) {
        sessions.forEach { repo.createSession(it) }
    }
}