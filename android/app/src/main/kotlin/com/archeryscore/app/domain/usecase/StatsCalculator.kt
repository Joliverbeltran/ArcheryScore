package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals

data class StatsAggregate(
    val totalSessions: Int,
    val totalArrows: Int,
    val totalScore: Int,
    val bestSessionScore: Int?,
    val averageScore: Int,
    val averageAccuracy: Int,
)

data class DisciplineStats(
    val discipline: Discipline,
    val sessionCount: Int,
    val averageScore: Int,
)

object StatsCalculator {

    fun aggregate(
        sessions: List<Session>,
        totals: Map<String, SessionTotals>,
    ): StatsAggregate {
        val complete = sessions.filter { it.status == com.archeryscore.app.domain.model.SessionStatus.COMPLETE }
        if (complete.isEmpty()) {
            return StatsAggregate(0, 0, 0, null, 0, 0)
        }
        val totalScore = complete.sumOf { session -> totals[session.id.toString()]?.total ?: 0 }
        val totalArrows = complete.sumOf { session -> totals[session.id.toString()]?.arrowsShot ?: 0 }
        val best = complete.maxOfOrNull { session -> totals[session.id.toString()]?.total ?: 0 }
        val average = totalScore / complete.size
        val accuracy = if (totalArrows == 0) 0 else totalScore * 10 / totalArrows
        return StatsAggregate(
            totalSessions = complete.size,
            totalArrows = totalArrows,
            totalScore = totalScore,
            bestSessionScore = best,
            averageScore = average,
            averageAccuracy = accuracy,
        )
    }

    fun byDiscipline(
        sessions: List<Session>,
        totals: Map<String, SessionTotals>,
    ): List<DisciplineStats> {
        val complete = sessions.filter { it.status == com.archeryscore.app.domain.model.SessionStatus.COMPLETE }
        return complete.groupBy { it.discipline }.map { (discipline, group) ->
            val sum = group.sumOf { totals[it.id.toString()]?.total ?: 0 }
            DisciplineStats(discipline, group.size, sum / group.size)
        }
    }
}