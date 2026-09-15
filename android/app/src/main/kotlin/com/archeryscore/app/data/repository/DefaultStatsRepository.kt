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

    override fun observeStats(userId: String, from: Instant?, to: Instant?): Flow<StatsSnapshot> =
        sessionRepository.observeSessions(userId).map { items ->
            val sessions = items.map { it.session }
                .filter { it.status == SessionStatus.COMPLETE }
                .let { StatsFilter.withinRange(it, from, to) }
            val totals = buildMap {
                sessions.forEach { s ->
                    sessionRepository.getTotals(s.id.toString())?.let { put(s.id.toString(), it) }
                }
            }
            StatsSnapshot(
                aggregate = StatsCalculator.aggregate(sessions, totals),
                byDiscipline = StatsCalculator.byDiscipline(sessions, totals),
                needsMoreData = StatsFilter.needsMoreData(sessions.size),
            )
        }
}
