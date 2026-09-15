package com.archeryscore.app.data.csv

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CsvExporterTest {

    private fun row(
        sessionId: String = UUID.randomUUID().toString(),
        date: Instant = Instant.parse("2026-09-08T09:15:00Z"),
        endNumber: Int = 1,
        arrowNumber: Int = 1,
        score: Int = 10,
        isXRing: Boolean = true,
        discipline: Discipline = Discipline.OLYMPIC_RECURVE,
    ) = CsvArrowRow(
        sessionId = sessionId,
        date = date,
        distanceM = 18,
        discipline = discipline,
        roundType = RoundType.TEN_ZONE,
        endNumber = endNumber,
        arrowNumber = arrowNumber,
        score = score,
        isXRing = isXRing,
    )

    @Test
    fun `header row matches exact contract`() {
        val lines = CsvExporter.export(listOf(row())).split("\r\n")
        assertEquals(
            "session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring",
            lines[0],
        )
    }

    @Test
    fun `escapes quotes commas and newlines per rfc 4180`() {
        val sessionId = "d29,1\"c\nsome"
        val csv = CsvExporter.export(listOf(row(sessionId = sessionId)))
        assertTrue(csv.contains("\"d29,1\"\"c\nsome\""))
    }

    @Test
    fun `orders rows chronologically then by end then arrow`() {
        val rows = listOf(
            row(endNumber = 1, arrowNumber = 2),
            row(date = Instant.parse("2026-09-09T00:00:00Z"), endNumber = 1, arrowNumber = 1),
            row(endNumber = 1, arrowNumber = 1),
            row(endNumber = 2, arrowNumber = 1),
        )
        val csv = CsvExporter.export(rows)
        val bodyLines = csv.split("\r\n").drop(1)
        assertEquals(4, bodyLines.size)
        assertEquals(1, bodyLines[0].split(",")[5].toInt())
        assertEquals(1, bodyLines[0].split(",")[6].toInt())
        assertEquals(1, bodyLines[1].split(",")[5].toInt())
        assertEquals(2, bodyLines[1].split(",")[6].toInt())
        assertEquals(2, bodyLines[2].split(",")[5].toInt())
        // Latest date sorts last.
        assertEquals("2026-09-09T00:00:00Z", bodyLines[3].split(",")[1])
    }

    @Test
    fun `empty input yields header only`() {
        assertEquals("session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring", CsvExporter.export(emptyList()))
    }

    @Test
    fun `dates are iso 8601 utc`() {
        val csv = CsvExporter.export(listOf(row()))
        val body = csv.split("\r\n")[1]
        assertEquals("2026-09-08T09:15:00Z", body.split(",")[1])
    }
}