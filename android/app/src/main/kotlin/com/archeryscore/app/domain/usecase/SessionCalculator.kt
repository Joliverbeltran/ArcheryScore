package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals

object SessionCalculator {

    fun runningTotal(arrows: List<Arrow>): RunningTotal {
        var total = 0
        var xCount = 0
        for (arrow in arrows) {
            total += arrow.score
            if (arrow.isXRing) xCount++
        }
        return RunningTotal(total, xCount)
    }

    fun sessionTotals(session: Session, arrows: List<Arrow>): SessionTotals {
        val byEnd = arrows.groupBy { it.endId }
        return SessionTotals(
            total = arrows.sumOf { it.score },
            xCount = arrows.count { it.isXRing },
            arrowsShot = arrows.size,
            endsShot = byEnd.size,
        )
    }

    fun isComplete(session: Session, arrows: List<Arrow>): Boolean {
        val endsShot = arrows.groupBy { it.endId }.size
        return endsShot >= session.endCount &&
            arrows.size >= session.endCount * session.arrowsPerEnd
    }
}

data class RunningTotal(
    val total: Int,
    val xCount: Int,
)