package com.archeryscore.app.data.repository

import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.data.local.entity.ArrowEntity
import com.archeryscore.app.data.local.entity.EndEntity
import com.archeryscore.app.data.sync.SyncOutboxWriter
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
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

    @Test
    fun `saveEnd persists the end under the arrows endId`() = runTest {
        val sessionDao = mockk<SessionDao>()
        val endDao = mockk<EndDao>()
        val arrowDao = mockk<ArrowDao>()
        val outbox = mockk<SyncOutboxWriter>()
        coEvery { sessionDao.upsert(any()) } just Runs
        coEvery { endDao.upsert(any()) } just Runs
        coEvery { arrowDao.upsertAll(any()) } just Runs
        coEvery { outbox.enqueue(any(), any(), any(), any(), any()) } just Runs

        val repo = RoomSessionRepository(
            sessionDao = sessionDao,
            endDao = endDao,
            arrowDao = arrowDao,
            syncWriteDao = mockk<SyncWriteDao>(),
            outbox = outbox,
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
}