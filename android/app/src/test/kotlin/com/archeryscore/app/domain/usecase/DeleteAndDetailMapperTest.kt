package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.End
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
import com.archeryscore.app.test.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteAndDetailMapperTest {

    private fun session() = Session(
        id = UUID.fromString("00000000-0000-0000-0000-00000000000b"),
        date = Instant.EPOCH,
        roundType = RoundType.TEN_ZONE,
        distanceM = 18,
        discipline = Discipline.LONGBOW,
        endCount = 2,
        arrowsPerEnd = 3,
        status = SessionStatus.COMPLETE,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `delete requires explicit confirmation`() = runTest {
        val repo = FakeSessionRepository()
        repo.createSession(session())
        val useCase = DeleteSessionUseCase(repo)

        val exception = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase.delete(session().id.toString()) }
        }
        assertTrue(exception.message.orEmpty().contains("confirmation"))
        assertTrue(repo.getSession(session().id.toString()) != null)
    }

    @Test
    fun `confirmed delete removes the session`() = runTest {
        val repo = FakeSessionRepository()
        repo.createSession(session())
        val useCase = DeleteSessionUseCase(repo)

        useCase.delete(session().id.toString(), confirmed = true)
        assertFalse(repo.getSession(session().id.toString()) != null)
    }

    @Test
    fun `detail mapper yields per end breakdowns with totals`() {
        val s = session()
        val endId = UUID.randomUUID()
        val arrows = listOf(
            Arrow(endId = endId, arrowNumber = 1, score = 10, isXRing = true, editedAt = Instant.EPOCH),
            Arrow(endId = endId, arrowNumber = 2, score = 9, editedAt = Instant.EPOCH),
            Arrow(endId = endId, arrowNumber = 3, score = 8, editedAt = Instant.EPOCH),
        )
        val detail = SessionDetail(
            session = s,
            ends = listOf(EndWithArrows(End(sessionId = s.id, endNumber = 1, createdAt = Instant.EPOCH), arrows)),
            total = 27,
            xCount = 1,
        )

        val breakdowns = SessionDetailMapper.toBreakdowns(detail)
        assertEquals(1, breakdowns.size)
        assertEquals(1, breakdowns[0].endNumber)
        assertEquals(listOf(10, 9, 8), breakdowns[0].arrows)
        assertEquals(27, breakdowns[0].total)
        assertEquals(1, breakdowns[0].xRingCount)
    }
}