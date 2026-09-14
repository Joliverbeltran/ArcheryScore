package com.archeryscore.app.data.csv

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.ImportedSession
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CsvImporterTest {

    private val now: Instant = Instant.parse("2026-09-12T10:00:00Z")

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

    private val header = "session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring"

    private fun importedToRows(sessions: List<ImportedSession>): List<CsvArrowRow> =
        sessions.flatMap { imported ->
            imported.ends.flatMap { end ->
                end.arrows.map { arrow ->
                    CsvArrowRow(
                        sessionId = imported.session.id.toString(),
                        date = imported.session.date,
                        distanceM = imported.session.distanceM,
                        discipline = imported.session.discipline,
                        roundType = imported.session.roundType,
                        endNumber = end.endNumber,
                        arrowNumber = arrow.arrowNumber,
                        score = arrow.score,
                        isXRing = arrow.isXRing,
                    )
                }
            }
        }

    @Test
    fun `round trip export to import to export is identical`() {
        val rows = listOf(
            row(endNumber = 1, arrowNumber = 1, score = 10, isXRing = true),
            row(endNumber = 1, arrowNumber = 2, score = 9, isXRing = false),
            row(endNumber = 2, arrowNumber = 1, score = 8, isXRing = false),
        )
        val csv = CsvExporter.export(rows)

        val result = CsvImporter.import(csv, now)

        val success = assertInstanceOf(CsvImportResult.Success::class.java, result)
        val reparsed = importedToRows(success.sessions)
        assertEquals(csv, CsvExporter.export(reparsed))
    }

    @Test
    fun `reconstructs session with derived end count and complete status`() {
        val sessionId = UUID.randomUUID().toString()
        val csv = CsvExporter.export(
            listOf(
                row(sessionId = sessionId, endNumber = 2, arrowNumber = 1, score = 9, isXRing = false),
                row(sessionId = sessionId, endNumber = 1, arrowNumber = 1, score = 10, isXRing = true),
                row(sessionId = sessionId, endNumber = 1, arrowNumber = 3, score = 7, isXRing = false),
            ),
        )

        val result = CsvImporter.import(csv, now)

        val success = assertInstanceOf(CsvImportResult.Success::class.java, result)
        assertEquals(1, success.sessions.size)
        val imported = success.sessions.single()
        assertEquals(2, imported.session.endCount)
        assertEquals(3, imported.session.arrowsPerEnd)
        assertEquals(SessionStatus.COMPLETE, imported.session.status)
        assertEquals(Instant.parse("2026-09-08T09:15:00Z"), imported.session.createdAt)
        assertEquals(now, imported.session.updatedAt)
        assertNull(imported.session.notes)
    }

    @Test
    fun `wrong header is rejected with structured error`() {
        val csv = "id,date\nabc,now"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("header", failure.error.column)
    }

    @Test
    fun `row with wrong field count is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals(1, failure.error.row)
    }

    @Test
    fun `invalid session id is rejected`() {
        val csv = "$header\nnot-a-uuid,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals(1, failure.error.row)
        assertEquals("session_id", failure.error.column)
    }

    @Test
    fun `invalid date is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},yesterday,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("date", failure.error.column)
    }

    @Test
    fun `distance out of range is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,305,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("distance_m", failure.error.column)
    }

    @Test
    fun `unknown discipline is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,SQUID,TEN_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("discipline", failure.error.column)
    }

    @Test
    fun `unknown round type is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TWENTY_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("round_type", failure.error.column)
    }

    @Test
    fun `zero end number is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,0,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("end_number", failure.error.column)
    }

    @Test
    fun `zero arrow number is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,0,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("arrow_number", failure.error.column)
    }

    @Test
    fun `score above round max is rejected for ten zone`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,11,false"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("score", failure.error.column)
    }

    @Test
    fun `score above round max is rejected for five zone`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,FIVE_ZONE,1,1,6,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("score", failure.error.column)
    }

    @Test
    fun `x ring requires score ten`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,9,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("is_x_ring", failure.error.column)
    }

    @Test
    fun `invalid is_x_ring value is rejected`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,yes"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("is_x_ring", failure.error.column)
    }

    @Test
    fun `atomic on first bad row - no partial rows returned`() {
        val goodSession = UUID.randomUUID().toString()
        val badSession = UUID.randomUUID().toString()
        val csv = "$header\r\n" +
            "$goodSession,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true\r\n" +
            "$badSession,admin,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals(2, failure.error.row)
    }

    @Test
    fun `header only file is rejected as no data rows`() {
        val result = CsvImporter.import(header, now)

        assertTrue(result is CsvImportResult.Failure)
    }

    @Test
    fun `empty file is rejected`() {
        val result = CsvImporter.import("", now)

        assertTrue(result is CsvImportResult.Failure)
    }

    @Test
    fun `oversized file over row limit is rejected`() {
        val manyRows = (1..100_001).joinToString("\r\n") { n ->
            "${UUID.randomUUID()},2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,$n,10,true"
        }
        val csv = "$header\r\n$manyRows"

        val result = CsvImporter.import(csv, now)

        assertTrue(result is CsvImportResult.Failure)
    }

    @Test
    fun `groups multiple sessions by id`() {
        val sessionA = UUID.randomUUID().toString()
        val sessionB = UUID.randomUUID().toString()
        val csv = CsvExporter.export(
            listOf(
                row(sessionId = sessionA, endNumber = 1, arrowNumber = 1, score = 10, isXRing = true),
                row(sessionId = sessionB, endNumber = 1, arrowNumber = 1, score = 8, isXRing = false),
                row(sessionId = sessionA, endNumber = 1, arrowNumber = 2, score = 9, isXRing = false),
            ),
        )

        val result = CsvImporter.import(csv, now)

        val success = assertInstanceOf(CsvImportResult.Success::class.java, result)
        assertEquals(2, success.sessions.size)
        assertNotNull(success.sessions.firstOrNull { it.session.id.toString() == sessionA })
        assertNotNull(success.sessions.firstOrNull { it.session.id.toString() == sessionB })
    }

    @Test
    fun `fails on quoted comma value that is not a valid uuid`() {
        val csv = "$header\n\"d29,1c\",2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true"

        val result = CsvImporter.import(csv, now)

        val failure = assertInstanceOf(CsvImportResult.Failure::class.java, result)
        assertEquals("session_id", failure.error.column)
    }

    @Test
    fun `imported sessions preserve discipline and round type`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,50,COMPOUND,FIVE_ZONE,1,1,5,false"

        val result = CsvImporter.import(csv, now)

        val success = assertInstanceOf(CsvImportResult.Success::class.java, result)
        val imported = success.sessions.single()
        assertEquals(Discipline.COMPOUND, imported.session.discipline)
        assertEquals(RoundType.FIVE_ZONE, imported.session.roundType)
        assertEquals(50, imported.session.distanceM)
    }

    @Test
    fun `reports no x ring for five zone where ten impossible and false given`() {
        val csv = "$header\n${UUID.randomUUID()},2026-09-08T09:15:00Z,50,COMPOUND,FIVE_ZONE,1,1,5,false"

        val result = CsvImporter.import(csv, now)

        val success = assertInstanceOf(CsvImportResult.Success::class.java, result)
        assertFalse(success.sessions.single().ends.single().arrows.single().isXRing)
    }
}