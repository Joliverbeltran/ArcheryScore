package com.archeryscore.app.data.repository

import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.repository.StatsRepository
import com.archeryscore.app.domain.repository.StatsSnapshot
import com.archeryscore.app.domain.usecase.StatsCalculator
import com.archeryscore.app.domain.usecase.StatsFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class DefaultStatsRepository(
    private val sessionRepository: SessionRepository,
) : StatsRepository {

    override fun observeStats(from: Instant?, to: Instant?): Flow<StatsSnapshot> =
        sessionRepository.observeSessions().map { sessions ->
            val completed = sessions
                .filter { it.status == SessionStatus.COMPLETE }
                .let { StatsFilter.withinRange(it, from, to) }
            val totals = buildMap {
                completed.forEach { s ->
                    sessionRepository.getTotals(s.id.toString())?.let { put(s.id.toString(), it) }
                }
            }
            StatsSnapshot(
                aggregate = StatsCalculator.aggregate(completed, totals),
                byDiscipline = StatsCalculator.byDiscipline(completed, totals),
                needsMoreData = StatsFilter.needsMoreData(completed.size),
            )
        }
}
