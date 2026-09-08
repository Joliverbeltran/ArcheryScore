package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class StatsFilterTest {

    private fun session(offsetDays: Long) = Session(
        id = java.util.UUID.randomUUID(),
        userId = "u1",
        date = Instant.parse("2026-09-08T12:00:00Z").plusSeconds(offsetDays * 86400),
        roundType = RoundType.TEN_ZONE,
        distanceM = 18,
        discipline = Discipline.COMPOUND,
        endCount = 6,
        arrowsPerEnd = 3,
        status = SessionStatus.COMPLETE,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `range filter keeps inclusive bounds`() {
        val early = session(0)
        val later = session(2)
        val from = Instant.parse("2026-09-08T00:00:00Z")
        val to = Instant.parse("2026-09-08T23:59:59Z")

        val filtered = StatsFilter.withinRange(listOf(early, later), from, to)

        assertEquals(listOf(early.id), filtered.map { it.id })
    }

    @Test
    fun `empty range boundaries select all`() {
        val s = session(1)
        assertEquals(listOf(s.id), StatsFilter.withinRange(listOf(s), null, null).map { it.id })
    }

    @Test
    fun `needs more data below threshold`() {
        assertTrue(StatsFilter.needsMoreData(2))
        assertFalse(StatsFilter.needsMoreData(3))
        assertFalse(StatsFilter.needsMoreData(5))
    }
}