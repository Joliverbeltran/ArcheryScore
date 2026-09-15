package com.archeryscore.app.data.repository

import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.ImportedArrow
import com.archeryscore.app.domain.model.ImportedEnd
import com.archeryscore.app.domain.model.ImportedSession
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.repository.ImportResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RoomSessionRepositoryTest {

    private fun session() = Session(
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

    @Test
    fun `saveEnd persists the end under the arrows endId`() = runTest {
        val sessionDao = mockk<SessionDao>()
        val endDao = mockk<EndDao>()
        val arrowDao = mockk<ArrowDao>()
        coEvery { sessionDao.upsert(any()) } just Runs
        coEvery { endDao.upsert(any()) } just Runs
        coEvery { arrowDao.upsertAll(any()) } just Runs

        val repo = RoomSessionRepository(
            sessionDao = sessionDao,
            endDao = endDao,
            arrowDao = arrowDao,
        )

        val endId = UUID.randomUUID()
        val arrows = (1..3).map { n ->
            Arrow(endId = endId, arrowNumber = n, score = 10, editedAt = Instant.EPOCH)
        }

        val saved = repo.saveEnd(session(), arrows, 1)

        assertEquals(endId, saved.id)

        val endEntity = slot<EndEntity>()
        coVerify { endDao.upsert(capture(endEntity)) }
        assertEquals(endId.toString(), endEntity.captured.id)

        val arrowEntities = slot<List<ArrowEntity>>()
        coVerify { arrowDao.upsertAll(capture(arrowEntities)) }
        assertEquals(3, arrowEntities.captured.size)
        assertTrue(arrowEntities.captured.all { it.endId == endId.toString() })
    }

    @Test
    fun `importSessions writes session ends and arrows with ids already persisted returns imported`() = runTest {
        val sessionDao = mockk<SessionDao>()
        val endDao = mockk<EndDao>()
        val arrowDao = mockk<ArrowDao>()
        coEvery { sessionDao.getById(any()) } returns null
        coEvery { sessionDao.upsert(any()) } just Runs
        coEvery { endDao.upsert(any()) } just Runs
        coEvery { arrowDao.upsert(any()) } just Runs

        val repo = RoomSessionRepository(sessionDao, endDao, arrowDao)

        val imported = ImportedSession(
            session = session().copy(status = SessionStatus.COMPLETE, endCount = 2, arrowsPerEnd = 2),
            ends = listOf(
                ImportedEnd(
                    endNumber = 1,
                    arrows = listOf(ImportedArrow(1, 10, true), ImportedArrow(2, 9, false)),
                ),
                ImportedEnd(
                    endNumber = 2,
                    arrows = listOf(ImportedArrow(1, 8, false)),
                ),
            ),
        )

        val result: ImportResult = repo.importSessions(listOf(imported))

        assertEquals(1, result.imported)
        assertEquals(0, result.skipped)
        coVerify(exactly = 1) { sessionDao.upsert(any()) }
        coVerify(exactly = 2) { endDao.upsert(any()) }
        coVerify(exactly = 3) { arrowDao.upsert(any()) }
    }

    @Test
    fun `importSessions skips sessions already present`() = runTest {
        val sessionDao = mockk<SessionDao>()
        val endDao = mockk<EndDao>()
        val arrowDao = mockk<ArrowDao>()
        coEvery { sessionDao.getById(any()) } returns com.archeryscore.app.data.local.entity.SessionEntity(
            id = "existing",
            date = 0,
            roundType = RoundType.TEN_ZONE.name,
            distanceM = 18,
            discipline = Discipline.OLYMPIC_RECURVE.name,
            endCount = 1,
            arrowsPerEnd = 1,
            notes = null,
            status = SessionStatus.COMPLETE.name,
            createdAt = 0,
            updatedAt = 0,
        )

        val repo = RoomSessionRepository(sessionDao, endDao, arrowDao)

        val imported = ImportedSession(
            session = session().copy(status = SessionStatus.COMPLETE, endCount = 1, arrowsPerEnd = 1),
            ends = listOf(
                ImportedEnd(endNumber = 1, arrows = listOf(ImportedArrow(1, 10, true))),
            ),
        )

        val result: ImportResult = repo.importSessions(listOf(imported))

        assertEquals(0, result.imported)
        assertEquals(1, result.skipped)
        coVerify(exactly = 0) { sessionDao.upsert(any()) }
        coVerify(exactly = 0) { endDao.upsert(any()) }
        coVerify(exactly = 0) { arrowDao.upsert(any()) }
    }
}