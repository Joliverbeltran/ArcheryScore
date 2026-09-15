package com.archeryscore.app.domain.usecase

import com.archeryscore.app.domain.model.Session
import java.time.Instant

object StatsFilter {

    const val MIN_SESSIONS_FOR_TREND = 3

    fun withinRange(sessions: List<Session>, from: Instant?, to: Instant?): List<Session> =
        sessions.filter { s ->
            val afterLower = from == null || !s.date.isBefore(from)
            val beforeUpper = to == null || !s.date.isAfter(to)
            afterLower && beforeUpper
        }

    fun needsMoreData(sessionCount: Int, threshold: Int = MIN_SESSIONS_FOR_TREND): Boolean =
        sessionCount < threshold
}