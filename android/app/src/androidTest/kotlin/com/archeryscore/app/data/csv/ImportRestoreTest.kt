package com.archeryscore.app.data.csv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.data.local.AppDatabase
import com.archeryscore.app.data.repository.RoomSessionRepository
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.TargetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ImportRestoreTest {

    @Test
    fun importFromExportedFile_restoresSessionsAndMakesThemVisible() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "import-restore-test.db"
        context.deleteDatabase(dbName)

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        try {
            val repo = RoomSessionRepository(
                sessionDao = db.sessionDao(),
                endDao = db.endDao(),
                arrowDao = db.arrowDao(),
            )

            runBlocking {
                val sessionId = UUID.randomUUID()
                val session = com.archeryscore.app.domain.model.Session(
                    id = sessionId,
                    date = Instant.parse("2026-09-08T09:15:00Z"),
                    roundType = RoundType.TEN_ZONE,
                    distanceM = 18,
                    discipline = Discipline.OLYMPIC_RECURVE,
                    endCount = 1,
                    arrowsPerEnd = 3,
                    notes = "sighted",
                    status = com.archeryscore.app.domain.model.SessionStatus.COMPLETE,
                    createdAt = Instant.parse("2026-09-08T09:15:00Z"),
                    updatedAt = Instant.parse("2026-09-08T09:20:00Z"),
                )
                repo.createSession(session)

                val sessionList = repo.observeSessions().first()
                val rows = mutableListOf<CsvArrowRow>()
                for (s in sessionList) {
                    for (end in db.endDao().getBySession(s.id.toString())) {
                        for (arrow in db.arrowDao().getForEnds(listOf(end.id))) {
                            rows.add(
                                CsvArrowRow(
                                    sessionId = s.id.toString(),
                                    date = s.date,
                                    distanceM = s.distanceM,
                                    discipline = s.discipline,
                                    roundType = s.roundType,
                                    targetType = s.targetType,
                                    endNumber = end.endNumber,
                                    arrowNumber = arrow.arrowNumber,
                                    score = arrow.score,
                                    isXRing = arrow.isXRing,
                                ),
                            )
                        }
                    }
                }
                val csv = CsvExporter.export(rows)
                val imported = (CsvImporter.import(csv, Instant.parse("2026-09-12T10:00:00Z")) as CsvImportResult.Success).sessions

                context.deleteDatabase(dbName)
                val restored = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
                val restoredRepo = RoomSessionRepository(restored.sessionDao(), restored.endDao(), restored.arrowDao())

                val result = restoredRepo.importSessions(imported)
                restored.close()
                assertEquals(1, result.imported)

                val visible = restoredRepo.observeSessions().first()
                assertEquals(1, visible.size)
                assertEquals(sessionId.toString(), visible.first().id.toString())
                assertEquals(3, visible.first().endCount * visible.first().arrowsPerEnd)
            }
        } finally {
            db.close()
            context.deleteDatabase(dbName)
        }
    }
}