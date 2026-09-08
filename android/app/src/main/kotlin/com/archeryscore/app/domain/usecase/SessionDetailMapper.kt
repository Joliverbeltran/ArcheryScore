package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.repository.SessionDetail

data class EndBreakdown(
    val endNumber: Int,
    val arrows: List<Int>,
    val total: Int,
    val xRingCount: Int,
)

object SessionDetailMapper {

    fun toBreakdowns(detail: SessionDetail): List<EndBreakdown> =
        detail.ends.map { e ->
            EndBreakdown(
                endNumber = e.end.endNumber,
                arrows = e.arrows.map { it.score },
                total = e.arrows.sumOf { it.score },
                xRingCount = e.arrows.count { it.isXRing },
            )
        }
}