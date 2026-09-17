package com.archeryscore.app.ui.record

import androidx.lifecycle.SavedStateHandle
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.TargetType
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionTargetPersistenceTest {

    private val repo = FakeSessionRepository()
    private val dispatcher = StandardTestDispatcher()

    private fun session(targetType: TargetType) = Session(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        date = Instant.EPOCH,
        roundType = RoundType.TEN_ZONE,
        targetType = targetType,
        distanceM = 18,
        discipline = Discipline.OLYMPIC_RECURVE,
        endCount = 6,
        arrowsPerEnd = 3,
        status = SessionStatus.ACTIVE,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun end(sessionId: UUID, number: Int, scores: List<Int>): EndWithArrows {
        val endId = UUID.randomUUID()
        val arrows = scores.mapIndexed { idx, score ->
            com.archeryscore.app.domain.model.Arrow(
                endId = endId,
                arrowNumber = idx + 1,
                score = score,
                editedAt = Instant.EPOCH,
            )
        }
        return EndWithArrows(
            End(id = endId, sessionId = sessionId, endNumber = number, createdAt = Instant.EPOCH),
            arrows,
        )
    }

    private suspend fun vm(targetType: TargetType): ActiveSessionViewModel {
        val s = session(targetType)
        repo.createSession(s)
        val existing = end(s.id, 1, listOf(0, 0, 0))
        repo.installDetail(SessionDetail(s, listOf(existing), total = 0, xCount = 0))
        return ActiveSessionViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to s.id.toString())),
            sessionRepository = repo,
            editScoreUseCase = EditScoreUseCase(repo),
        )
    }

    @BeforeEach
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `CM80 type surfaces through placement for every arrow in the end`() = runTest(dispatcher.scheduler) {
        val vm = vm(TargetType.CM80)
        advanceUntilIdle()

        val arrows = vm.detail.value!!.ends.first().arrows

        arrows.forEach { arrow ->
            vm.startPlacement(arrow)
            advanceUntilIdle()
            vm.updatePending(PlacementPoint(0f, 0f))
            vm.confirmPlacement()
            advanceUntilIdle()
        }

        assertEquals(listOf(10, 10, 10), repo.recordedUpdates.map { it.score })
        assertTrue(repo.recordedUpdates.all { it.isXRing })
    }

    @Test
    fun `TRIPLE_VERTICAL type surfaces through placement for every arrow in the end`() = runTest(dispatcher.scheduler) {
        val vm = vm(TargetType.TRIPLE_VERTICAL)
        advanceUntilIdle()

        val arrows = vm.detail.value!!.ends.first().arrows

        arrows.forEach { arrow ->
            vm.startPlacement(arrow)
            advanceUntilIdle()
            vm.updatePending(PlacementPoint(0f, 0f))
            vm.confirmPlacement()
            advanceUntilIdle()
        }

        assertEquals(listOf(10, 10, 10), repo.recordedUpdates.map { it.score })
        assertTrue(repo.recordedUpdates.all { it.isXRing })
    }
}
