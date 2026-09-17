package com.archeryscore.app.data.csv

import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.TargetType
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class CsvArrowRow(
    val sessionId: String,
    val date: Instant,
    val distanceM: Int,
    val discipline: Discipline,
    val roundType: RoundType,
    val targetType: TargetType,
    val endNumber: Int,
    val arrowNumber: Int,
    val score: Int,
    val isXRing: Boolean,
)

object CsvExporter {

    private val HEADER = "session_id,date,distance_m,discipline,round_type,target_type,end_number,arrow_number,score,is_x_ring"
    private val ISO_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun export(rows: List<CsvArrowRow>): String {
        val sorted = rows.sortedWith(
            compareBy<CsvArrowRow> { it.date }
                .thenBy { it.endNumber }
                .thenBy { it.arrowNumber },
        )
        val body = sorted.joinToString("\r\n") { row ->
            listOf(
                row.sessionId,
                ISO_UTC.format(row.date),
                row.distanceM.toString(),
                row.discipline.name,
                row.roundType.name,
                row.targetType.name,
                row.endNumber.toString(),
                row.arrowNumber.toString(),
                row.score.toString(),
                row.isXRing.toString(),
            ).joinToString(",") { escape(it) }
        }
        return if (sorted.isEmpty()) HEADER else "$HEADER\r\n$body"
    }

    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
}