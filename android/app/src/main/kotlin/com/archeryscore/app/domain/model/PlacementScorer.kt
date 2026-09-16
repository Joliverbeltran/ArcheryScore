package com.archeryscore.app.domain.model

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

data class PlacementResult(
    val score: Int,
    val isXRing: Boolean,
    val miss: Boolean,
    val spotIndex: Int?,
)

object PlacementScorer {

    fun place(
        x: Float,
        y: Float,
        targetType: TargetType,
        scoringType: RoundType,
    ): PlacementResult {
        val (centerX, centerY, spotIndex) = resolveFace(x, y, targetType)
        val dx = x - centerX
        val dy = y - centerY
        val r = sqrt(dx * dx + dy * dy)

        val maxScore = scoringType.maxScore
        val w = 1f / maxScore

        if (r >= 1f) {
            return PlacementResult(score = 0, isXRing = false, miss = true, spotIndex = spotIndex)
        }

        val band = maxScore + 1 - ceil(r / w).toInt()
        val score = band.coerceIn(1, maxScore)
        val isXRing = scoringType == RoundType.TEN_ZONE && score == maxScore && r <= 0.5f * w

        return PlacementResult(score = score, isXRing = isXRing, miss = false, spotIndex = spotIndex)
    }

    private fun resolveFace(x: Float, y: Float, targetType: TargetType): Triple<Float, Float, Int?> {
        if (targetType.spotCount == 1) {
            return Triple(0f, 0f, null)
        }
        var nearest = 0
        var nearestDistance = Float.MAX_VALUE
        targetType.spotCenters.forEachIndexed { index, (cx, cy) ->
            val dx = x - cx
            val dy = y - cy
            val distance = dx * dx + dy * dy
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = index
            }
        }
        val center = targetType.spotCenters[nearest]
        return Triple(center.first, center.second, nearest)
    }
}