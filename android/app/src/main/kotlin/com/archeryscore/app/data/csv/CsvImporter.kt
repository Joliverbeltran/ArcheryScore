package com.archeryscore.app.data.csv

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.ImportedArrow
import com.archeryscore.app.domain.model.ImportedEnd
import com.archeryscore.app.domain.model.ImportedSession
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.TargetType
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

sealed interface CsvImportResult {
    data class Success(val sessions: List<ImportedSession>) : CsvImportResult
    data class Failure(val error: CsvImportError) : CsvImportResult
}

data class CsvImportError(
    val row: Int,
    val column: String,
    val reason: String,
)

object CsvImporter {

    private val HEADER =
        "session_id,date,distance_m,discipline,round_type,target_type,end_number,arrow_number,score,is_x_ring"
    private val LEGACY_HEADER =
        "session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring"
    private val ISO_UTC =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    private const val MAX_BYTES = 5 * 1024 * 1024
    private const val MAX_ROWS = 100_000

    fun import(csv: String, now: Instant = Instant.now()): CsvImportResult {
        errorRow = null
        if (csv.isEmpty()) {
            return failure(0, "file", "empty")
        }
        if (csv.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
            return failure(0, "file", "too_large")
        }

        val records = parseRfc4180(csv)
        if (records == null) {
            return failure(0, "file", "malformed")
        }

        val header = records[0].joinToString(",")
        val cols10 = header == HEADER
        val cols9 = header == LEGACY_HEADER
        if (!cols10 && !cols9) {
            return failure(1, "header", "invalid_header")
        }
        val fieldCount = if (cols10) 10 else 9

        val dataRecords = records.drop(1)
        if (dataRecords.isEmpty()) {
            return failure(0, "file", "no_data_rows")
        }
        if (dataRecords.size > MAX_ROWS) {
            return failure(0, "file", "too_many_rows")
        }

        val validRows = mutableListOf<CsvArrowRow>()
        for (index in dataRecords.indices) {
            val rowNumber = index + 1
            val fields = dataRecords[index]
            if (fields.size != fieldCount) {
                return failure(rowNumber, "file", "field_count")
            }
            val row = buildRow(fields, rowNumber, cols10) ?: return errorResult(rowNumber)
            validRows += row
        }

        return CsvImportResult.Success(buildSessions(validRows, now))
    }

    private fun buildSessions(rows: List<CsvArrowRow>, now: Instant): List<ImportedSession> =
        rows.groupBy { it.sessionId }
            .map { (sessionId, sessionRows) ->
                val first = sessionRows.first()
                val session = Session(
                    id = UUID.fromString(sessionId),
                    date = first.date,
                    roundType = first.roundType,
                    targetType = first.targetType,
                    distanceM = first.distanceM,
                    discipline = first.discipline,
                    endCount = sessionRows.maxOf { it.endNumber },
                    arrowsPerEnd = sessionRows.maxOf { it.arrowNumber },
                    notes = null,
                    status = SessionStatus.COMPLETE,
                    createdAt = first.date,
                    updatedAt = now,
                )
                val ends = sessionRows
                    .groupBy { it.endNumber }
                    .map { (endNumber, endRows) ->
                        ImportedEnd(
                            endNumber = endNumber,
                            arrows = endRows.map { row ->
                                ImportedArrow(row.arrowNumber, row.score, row.isXRing)
                            }.sortedBy { it.arrowNumber },
                        )
                    }
                    .sortedBy { it.endNumber }
                ImportedSession(session, ends)
            }

    private fun buildRow(fields: List<String>, rowNumber: Int, cols10: Boolean): CsvArrowRow? {
        val sessionId = fields[0]
        if (!isUuid(sessionId)) {
            errorRow = CsvImportError(rowNumber, "session_id", "invalid_uuid")
            return null
        }
        val date = parseDate(fields[1])
        if (date == null) {
            errorRow = CsvImportError(rowNumber, "date", "invalid_date")
            return null
        }
        val distanceM = fields[2].toIntOrNull()
        if (distanceM == null || distanceM !in 8..300) {
            errorRow = CsvImportError(rowNumber, "distance_m", "invalid_distance")
            return null
        }
        val discipline = enumOrNull<Discipline>(fields[3])
        if (discipline == null) {
            errorRow = CsvImportError(rowNumber, "discipline", "invalid_discipline")
            return null
        }
        val roundType = enumOrNull<RoundType>(fields[4])
        if (roundType == null) {
            errorRow = CsvImportError(rowNumber, "round_type", "invalid_round_type")
            return null
        }
        val targetType = if (cols10) {
            val t = enumOrNull<TargetType>(fields[5])
            if (t == null) {
                errorRow = CsvImportError(rowNumber, "target_type", "invalid_target_type")
                return null
            }
            t
        } else {
            TargetType.CM122
        }
        val endNumber = fields[5 + if (cols10) 1 else 0].toIntOrNull()
        if (endNumber == null || endNumber < 1) {
            errorRow = CsvImportError(rowNumber, "end_number", "invalid_end_number")
            return null
        }
        val arrowNumber = fields[6 + if (cols10) 1 else 0].toIntOrNull()
        if (arrowNumber == null || arrowNumber < 1) {
            errorRow = CsvImportError(rowNumber, "arrow_number", "invalid_arrow_number")
            return null
        }
        val score = fields[7 + if (cols10) 1 else 0].toIntOrNull()
        if (score == null || score !in 1..roundType.maxScore) {
            errorRow = CsvImportError(rowNumber, "score", "invalid_score")
            return null
        }
        val isXRing = when (fields[8 + if (cols10) 1 else 0]) {
            "true" -> true
            "false" -> false
            else -> {
                errorRow = CsvImportError(rowNumber, "is_x_ring", "invalid_x_ring")
                return null
            }
        }
        if (isXRing && score != 10) {
            errorRow = CsvImportError(rowNumber, "is_x_ring", "x_ring_requires_ten")
            return null
        }
        return CsvArrowRow(
            sessionId = sessionId,
            date = date,
            distanceM = distanceM,
            discipline = discipline,
            roundType = roundType,
            targetType = targetType,
            endNumber = endNumber,
            arrowNumber = arrowNumber,
            score = score,
            isXRing = isXRing,
        )
    }

    private var errorRow: CsvImportError? = null

    private fun errorResult(rowNumber: Int): CsvImportResult.Failure {
        val error = errorRow ?: CsvImportError(rowNumber, "file", "invalid")
        return CsvImportResult.Failure(error)
    }

    private fun failure(row: Int, column: String, reason: String): CsvImportResult.Failure =
        CsvImportResult.Failure(CsvImportError(row, column, reason))

    private fun isUuid(s: String): Boolean =
        runCatching { UUID.fromString(s) }.getOrNull()?.toString() == s

    private fun parseDate(s: String): Instant? =
        try {
            ISO_UTC.parse(s, Instant::from)
        } catch (e: DateTimeParseException) {
            null
        }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }

    private fun parseRfc4180(csv: String): List<List<String>>? {
        val records = mutableListOf<List<String>>()
        val fields = mutableListOf<String>()
        var field = StringBuilder()
        var inQuotes = false
        var endedWithNewline = false
        var index = 0
        while (index < csv.length) {
            val ch = csv[index]
            endedWithNewline = false
            when {
                inQuotes -> when {
                    ch == '"' && index + 1 < csv.length && csv[index + 1] == '"' -> {
                        field.append('"')
                        index++
                    }
                    ch == '"' -> inQuotes = false
                    else -> field.append(ch)
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> {
                    fields += field.toString()
                    field = StringBuilder()
                }
                ch == '\n' -> {
                    fields += field.toString()
                    records += fields.toList()
                    fields.clear()
                    field = StringBuilder()
                    endedWithNewline = true
                }
                ch == '\r' -> {
                    // CRLF handling: ignore \r before \n.
                }
                else -> field.append(ch)
            }
            index++
        }
        if (inQuotes) return null
        if (!endedWithNewline) {
            fields += field.toString()
            if (fields.isNotEmpty() || records.isEmpty()) {
                records += fields.toList()
            }
        }
        return records
    }
}