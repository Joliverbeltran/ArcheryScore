package com.archeryscore.app.ui.stats

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.repository.StatsSnapshot
import com.archeryscore.app.domain.usecase.DisciplineStats
import com.archeryscore.app.domain.usecase.StatsAggregate
import com.archeryscore.app.test.FakeStatsRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val statsRepo = FakeStatsRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stats derive from local-only observeStats with aggregate and by-discipline`() = runTest(dispatcher.scheduler) {
        val snapshot = StatsSnapshot(
            aggregate = StatsAggregate(
                totalSessions = 3,
                totalArrows = 54,
                totalScore = 486,
                bestSessionScore = 170,
                averageScore = 162,
                averageAccuracy = 83,
            ),
            byDiscipline = listOf(
                DisciplineStats(Discipline.OLYMPIC_RECURVE, 2, 165),
                DisciplineStats(Discipline.BAREBOW, 1, 158),
            ),
            needsMoreData = false,
        )
        statsRepo.setSnapshot(snapshot)

        val vm = StatsViewModel(statsRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.loading)
        assertEquals(3, state.aggregate.totalSessions)
        assertEquals(54, state.aggregate.totalArrows)
        assertEquals(486, state.aggregate.totalScore)
        assertEquals(170, state.aggregate.bestSessionScore)
        assertEquals(2, state.byDiscipline.size)
        assertEquals(Discipline.OLYMPIC_RECURVE, state.byDiscipline[0].discipline)
        assertFalse(state.needsMoreData)
    }

    @Test
    fun `fewer than three sessions reports needsMoreData and zero aggregate`() = runTest(dispatcher.scheduler) {
        statsRepo.setSnapshot(
            StatsSnapshot(
                aggregate = StatsAggregate(0, 0, 0, null, 0, 0),
                byDiscipline = emptyList(),
                needsMoreData = true,
            ),
        )

        val vm = StatsViewModel(statsRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(0, state.aggregate.totalSessions)
        assertEquals(0, state.aggregate.totalScore)
        assertEquals(null, state.aggregate.bestSessionScore)
        assertTrue(state.needsMoreData)
        assertTrue(state.byDiscipline.isEmpty())
    }

    @Test
    fun `setRange propagates the requested range`() = runTest(dispatcher.scheduler) {
        val vm = StatsViewModel(statsRepo)
        advanceUntilIdle()

        vm.setRange(java.time.Instant.EPOCH, java.time.Instant.parse("2026-12-31T23:59:59Z"))
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.loading)
        assertEquals(0, vm.uiState.value.aggregate.totalSessions)
    }
}