package com.archeryscore.app.data.csv

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.TargetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CsvRoundTripTargetTypeTest {

    private fun row(
        targetType: TargetType = TargetType.CM122,
    ) = CsvArrowRow(
        sessionId = UUID.randomUUID().toString(),
        date = Instant.parse("2026-09-08T09:15:00Z"),
        distanceM = 18,
        discipline = Discipline.OLYMPIC_RECURVE,
        roundType = RoundType.TEN_ZONE,
        targetType = targetType,
        endNumber = 1,
        arrowNumber = 1,
        score = 10,
        isXRing = true,
    )

    @Test
    fun `header row has 10 columns including target_type`() {
        val lines = CsvExporter.export(listOf(row())).split("\r\n")
        assertEquals(
            "session_id,date,distance_m,discipline,round_type,target_type,end_number,arrow_number,score,is_x_ring",
            lines[0],
        )
    }

    @Test
    fun `export includes target_type value`() {
        val csv = CsvExporter.export(listOf(row(targetType = TargetType.CM80)))
        val body = csv.split("\r\n")[1]
        assertEquals("CM80", body.split(",")[5])
    }

    @Test
    fun `round trip byte identical`() {
        val original = CsvExporter.export(listOf(row(targetType = TargetType.CM122)))
        val imported = CsvImporter.import(original)
        assertTrue(imported is CsvImportResult.Success)
        val reExported = CsvExporter.export(
            (imported as CsvImportResult.Success).sessions.flatMap { s ->
                s.ends.flatMap { e ->
                    e.arrows.map { a ->
                        CsvArrowRow(
                            sessionId = s.session.id.toString(),
                            date = s.session.date,
                            distanceM = s.session.distanceM,
                            discipline = s.session.discipline,
                            roundType = s.session.roundType,
                            targetType = s.session.targetType,
                            endNumber = e.endNumber,
                            arrowNumber = a.arrowNumber,
                            score = a.score,
                            isXRing = a.isXRing,
                        )
                    }
                }
            }
        )
        assertEquals(original, reExported)
    }

    @Test
    fun `legacy 9 column import defaults target_type to CM122`() {
        val legacyCsv = "session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring\r\n" +
            "${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true"
        val result = CsvImporter.import(legacyCsv)
        assertTrue(result is CsvImportResult.Success)
        val session = (result as CsvImportResult.Success).sessions.first().session
        assertEquals(TargetType.CM122, session.targetType)
    }
}
