package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.test.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class EditScoreUseCaseTest {

    private fun session() = Session(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        userId = "u1",
        date = Instant.EPOCH,
        roundType = RoundType.TEN_ZONE,
        distanceM = 18,
        discipline = Discipline.OLYMPIC_RECURVE,
        endCount = 6,
        arrowsPerEnd = 3,
        status = SessionStatus.ACTIVE,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun arrow(score: Int = 9) = Arrow(
        id = UUID.fromString("00000000-0000-0000-0000-00000000000a"),
        endId = UUID.randomUUID(),
        arrowNumber = 1,
        score = score,
        editedAt = Instant.EPOCH,
    )

    @Test
    fun `edit changes score and preserves identity`() = runTest {
        val repo = FakeSessionRepository()
        repo.createSession(session())
        val useCase = EditScoreUseCase(repo)

        val updated = useCase.edit(
            sessionId = "00000000-0000-0000-0000-000000000001",
            arrow = arrow(),
            newScore = 10,
        )

        assertEquals(10, updated.score)
        assertEquals(arrow().id, updated.id)
    }

    @Test
    fun `edit rejects out of range scores`() = runTest {
        val repo = FakeSessionRepository()
        repo.createSession(session())
        val useCase = EditScoreUseCase(repo)

        var thrown = false
        try {
            useCase.edit("00000000-0000-0000-0000-000000000001", arrow(), newScore = 11)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `edit preserves x ring flag only for ten in ten zone`() = runTest {
        val repo = FakeSessionRepository()
        repo.createSession(session())
        val useCase = EditScoreUseCase(repo)

        val ten = useCase.edit("00000000-0000-0000-0000-000000000001", arrow(), 10, isXRing = true)
        assertTrue(ten.isXRing)

        var rejected = false
        try {
            useCase.edit("00000000-0000-0000-0000-000000000001", arrow(), 8, isXRing = true)
        } catch (e: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertFalse(ten.score == 8)
    }

    @Test
    fun `edit on unknown session throws`() = runTest {
        val repo = FakeSessionRepository()
        val useCase = EditScoreUseCase(repo)

        var thrown = false
        try {
            useCase.edit("00000000-0000-0000-0000-00000000dead", arrow(), 9)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}