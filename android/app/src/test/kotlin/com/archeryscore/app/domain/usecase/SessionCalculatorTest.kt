package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionTotals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SessionCalculatorTest {

    private fun arrow(endId: UUID, n: Int, score: Int, xRing: Boolean = false) = Arrow(
        id = UUID.randomUUID(),
        endId = endId,
        arrowNumber = n,
        score = score,
        isXRing = xRing,
        editedAt = Instant.EPOCH,
    )

    @Test
    fun `running total sums arrows in order`() {
        val endId = UUID.randomUUID()
        val arrows = listOf(
            arrow(endId, 1, 10, xRing = true),
            arrow(endId, 2, 8),
            arrow(endId, 3, 5),
        )
        SessionCalculator.runningTotal(arrows).also {
            assertEquals(23, it.total)
            assertEquals(1, it.xCount)
        }
    }

    @Test
    fun `empty arrow list sums to zero`() {
        SessionCalculator.runningTotal(emptyList()).also {
            assertEquals(0, it.total)
            assertEquals(0, it.xCount)
        }
    }

    @Test
    fun `session totals aggregate all ends`() {
        val session = Session(
            userId = "u1",
            date = Instant.EPOCH,
            roundType = com.archeryscore.app.domain.model.RoundType.TEN_ZONE,
            distanceM = 18,
            discipline = com.archeryscore.app.domain.model.Discipline.BAREBOW,
            endCount = 2,
            arrowsPerEnd = 3,
            status = com.archeryscore.app.domain.model.SessionStatus.COMPLETE,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val end1 = UUID.randomUUID()
        val end2 = UUID.randomUUID()
        val arrows = listOf(
            arrow(end1, 1, 10, true),
            arrow(end1, 2, 9),
            arrow(end1, 3, 8),
            arrow(end2, 1, 7),
            arrow(end2, 2, 6),
            arrow(end2, 3, 5),
        )
        val totals: SessionTotals = SessionCalculator.sessionTotals(session, arrows)
        assertEquals(45, totals.total)
        assertEquals(1, totals.xCount)
        assertEquals(6, totals.arrowsShot)
        assertEquals(2, totals.endsShot)
    }

    @Test
    fun `completion requires all ends shot`() {
        val session = Session(
            userId = "u1",
            date = Instant.EPOCH,
            roundType = com.archeryscore.app.domain.model.RoundType.TEN_ZONE,
            distanceM = 18,
            discipline = com.archeryscore.app.domain.model.Discipline.BAREBOW,
            endCount = 3,
            arrowsPerEnd = 3,
            status = com.archeryscore.app.domain.model.SessionStatus.ACTIVE,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val endId = UUID.randomUUID()
        val arrows = (1..3).map { arrow(endId, it, 8) }
        assertTrue(!SessionCalculator.isComplete(session, arrows))
    }

    @Test
    fun `running total is proportional and never exceeds max`() {
        val session = Session(
            userId = "u1",
            date = Instant.EPOCH,
            roundType = com.archeryscore.app.domain.model.RoundType.TEN_ZONE,
            distanceM = 70,
            discipline = com.archeryscore.app.domain.model.Discipline.OLYMPIC_RECURVE,
            endCount = 2,
            arrowsPerEnd = 3,
            status = com.archeryscore.app.domain.model.SessionStatus.COMPLETE,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val endId = UUID.randomUUID()
        val perfect = (1..6).map { arrow(endId, it, 10, true) }
        assertEquals(60, SessionCalculator.sessionTotals(session, perfect).total)
    }
}