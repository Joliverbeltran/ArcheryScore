package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SessionTotals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class StatsCalculatorTest {

    private fun session(
        id: String = UUID.randomUUID().toString(),
        discipline: Discipline = Discipline.BAREBOW,
        status: SessionStatus = SessionStatus.COMPLETE,
    ) = Session(
        id = UUID.fromString(id),
        date = Instant.EPOCH,
        roundType = RoundType.TEN_ZONE,
        distanceM = 18,
        discipline = discipline,
        endCount = 2,
        arrowsPerEnd = 3,
        status = status,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun totals(total: Int, arrows: Int) = SessionTotals(
        total = total,
        xCount = 0,
        arrowsShot = arrows,
        endsShot = 2,
    )

    @Test
    fun `aggregate over empty history returns zeros`() {
        val agg = StatsCalculator.aggregate(emptyList(), emptyMap())
        assertEquals(0, agg.totalSessions)
        assertNull(agg.bestSessionScore)
    }

    @Test
    fun `aggregate counts only complete sessions`() {
        val complete = session("00000000-0000-0000-0000-000000000001")
        val active = session(
            "00000000-0000-0000-0000-000000000002",
            status = SessionStatus.ACTIVE,
        )
        val agg = StatsCalculator.aggregate(
            listOf(complete, active),
            mapOf(
                complete.id.toString() to totals(45, 6),
                active.id.toString() to totals(10, 3),
            ),
        )
        assertEquals(1, agg.totalSessions)
        assertEquals(45, agg.totalScore)
        assertEquals(45, agg.bestSessionScore)
        assertEquals(45, agg.averageScore)
    }

    @Test
    fun `average accuracy is score per arrow scaled to hundred`() {
        val s = session("00000000-0000-0000-0000-000000000001")
        val agg = StatsCalculator.aggregate(
            listOf(s),
            mapOf(s.id.toString() to totals(total = 48, arrows = 6)),
        )
        assertEquals(80, agg.averageAccuracy)
    }

    @Test
    fun `by discipline groups and averages`() {
        val bow = session("00000000-0000-0000-0000-000000000001", Discipline.BAREBOW)
        val compound = session("00000000-0000-0000-0000-000000000002", Discipline.COMPOUND)
        val byDiscipline = StatsCalculator.byDiscipline(
            listOf(bow, compound),
            mapOf(
                bow.id.toString() to totals(45, 6),
                compound.id.toString() to totals(55, 6),
            ),
        )
        assertEquals(2, byDiscipline.size)
        assertEquals(55, byDiscipline.first { it.discipline == Discipline.COMPOUND }.averageScore)
        assertEquals(45, byDiscipline.first { it.discipline == Discipline.BAREBOW }.averageScore)
    }

    @Test
    fun `total arrows sums across sessions`() {
        val a = session("00000000-0000-0000-0000-000000000001")
        val b = session("00000000-0000-0000-0000-000000000002")
        val agg = StatsCalculator.aggregate(
            listOf(a, b),
            mapOf(
                a.id.toString() to totals(45, 6),
                b.id.toString() to totals(55, 6),
            ),
        )
        assertEquals(12, agg.totalArrows)
        assertEquals(100, agg.totalScore)
        assertEquals(50, agg.averageScore)
    }
}