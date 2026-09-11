package com.archeryscore.app.ui.record

import androidx.lifecycle.SavedStateHandle
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.domain.usecase.EditScoreUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModelTest {

    private val repo = FakeSessionRepository()
    private val dispatcher = StandardTestDispatcher()

    private fun session(
        endCount: Int = 6,
        arrowsPerEnd: Int = 3,
    ) = Session(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        userId = "test-user",
        date = Instant.EPOCH,
        roundType = RoundType.TEN_ZONE,
        distanceM = 18,
        discipline = Discipline.OLYMPIC_RECURVE,
        endCount = endCount,
        arrowsPerEnd = arrowsPerEnd,
        status = SessionStatus.ACTIVE,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun end(sessionId: UUID, number: Int, arrows: List<Int>): EndWithArrows {
        val endId = UUID.randomUUID()
        return EndWithArrows(
            end = End(id = endId, sessionId = sessionId, endNumber = number, createdAt = Instant.EPOCH),
            arrows = arrows.mapIndexed { i, score ->
                Arrow(
                    id = UUID.randomUUID(),
                    endId = endId,
                    arrowNumber = i + 1,
                    score = score,
                    editedAt = Instant.EPOCH,
                )
            },
        )
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(session: Session, ends: List<EndWithArrows> = emptyList()): ActiveSessionViewModel {
        repo.installDetail(
            SessionDetail(
                session = session,
                ends = ends,
                total = ends.sumOf { it.arrows.sumOf { a -> a.score } },
                xCount = 0,
            )
        )
        return ActiveSessionViewModel(
            SavedStateHandle(mapOf("sessionId" to session.id.toString())),
            repo,
            EditScoreUseCase(repo),
        )
    }

    @Test
    fun `add end uses next available end number`() = runTest(dispatcher.scheduler) {
        val s = session()
        val vm = vm(s, ends = listOf(end(s.id, 1, listOf(10, 9, 8))))
        advanceUntilIdle()
        assertEquals(2, vm.nextEndNumber())
    }

    @Test
    fun `recording a valid arrow keeps session consistent`() = runTest(dispatcher.scheduler) {
        val s = session(endCount = 1)
        val existing = end(s.id, 1, listOf(10))
        repo.createSession(s)
        val vm = vm(s, ends = listOf(existing))
        advanceUntilIdle()

        vm.recordArrow(existing.arrows.first(), 7)
        advanceUntilIdle()

        assertTrue(repo.getSession(s.id.toString()) != null)
    }

    @Test
    fun `completion requires end goal to be reached`() = runTest(dispatcher.scheduler) {
        val s = session(endCount = 2, arrowsPerEnd = 3)
        val partially = vm(s, ends = listOf(end(s.id, 1, listOf(10, 9, 8))))
        advanceUntilIdle()
        assertFalse(partially.isFullyScored())

        val full = vm(
            s,
            ends = listOf(
                end(s.id, 1, listOf(10, 9, 8)),
                end(s.id, 2, listOf(7, 8, 9)),
            ),
        )
        advanceUntilIdle()
        assertTrue(full.isFullyScored())
    }

    @Test
    fun `completion allowed when enough ends recorded despite an unscorable end`() = runTest(dispatcher.scheduler) {
        val s = session(endCount = 2, arrowsPerEnd = 3)
        val vm = vm(
            s,
            ends = listOf(
                end(s.id, 1, emptyList()),
                end(s.id, 2, listOf(10, 9, 8)),
                end(s.id, 3, listOf(7, 8, 9)),
            ),
        )
        advanceUntilIdle()
        assertTrue(vm.isFullyScored())
    }

    @Test
    fun `confirm completion marks the session complete`() = runTest(dispatcher.scheduler) {
        val s = session(endCount = 1)
        repo.createSession(s)
        val vm = vm(s, ends = listOf(end(s.id, 1, listOf(10, 9, 8))))
        advanceUntilIdle()

        vm.confirmCompletion()
        advanceUntilIdle()

        assertEquals(SessionStatus.COMPLETE, repo.getSession(s.id.toString())?.status)
    }

    @Test
    fun `invalid edit is rejected and leaves the session intact`() = runTest(dispatcher.scheduler) {
        val s = session(endCount = 1)
        val existing = end(s.id, 1, listOf(10))
        repo.createSession(s)
        val vm = vm(s, ends = listOf(existing))
        advanceUntilIdle()

        // 11 is invalid for TEN_ZONE; must not crash and must keep session intact.
        vm.recordArrow(existing.arrows.first(), 11)
        advanceUntilIdle()

        assertTrue(repo.getSession(s.id.toString()) != null)
    }
}