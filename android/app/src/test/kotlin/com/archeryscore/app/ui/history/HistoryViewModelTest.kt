package com.archeryscore.app.ui.history

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
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

        val vm = HistoryViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.sessions.size)
        assertEquals(newer.id, state.sessions[0].id)
        assertEquals(older.id, state.sessions[1].id)
    }

    @Test
    fun `empty repository yields empty session list`() = runTest(dispatcher.scheduler) {
        val vm = HistoryViewModel(repo)
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

    @Test
    fun `importCsv reports imported and skipped counts`() = runTest(dispatcher.scheduler) {
        val vm = HistoryViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val sessionId = UUID.randomUUID().toString()
        vm.importCsv(
            CsvHeader +
                "\r\n$sessionId,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true",
        )
        dispatcher.scheduler.advanceUntilIdle()

        val message = vm.importMessage.value as ImportMessage.Success
        assertEquals(1, message.imported)
        assertEquals(0, message.skipped)
    }

    @Test
    fun `importCsv reports validation error with row column and reason`() = runTest(dispatcher.scheduler) {
        val vm = HistoryViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.importCsv("session_id,date\nnot-a-date")
        dispatcher.scheduler.advanceUntilIdle()

        val message = vm.importMessage.value as ImportMessage.Failure
        assertTrue(message.row > 0)
        assertTrue(message.column.isNotEmpty())
        assertTrue(message.reason.isNotEmpty())
    }

    private suspend fun runTestFactor(repo: FakeSessionRepository, vararg sessions: Session) {
        sessions.forEach { repo.createSession(it) }
    }

    private companion object {
        const val CsvHeader =
            "session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring"
    }
}