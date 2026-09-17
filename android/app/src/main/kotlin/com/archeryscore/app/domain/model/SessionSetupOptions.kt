package com.archeryscore.app.domain.model

import kotlin.math.abs

object SessionSetupOptions {

    val DISTANCES_M: List<Int> = listOf(8, 12, 18, 30, 40, 50, 70, 90)
    val ARROWS_PER_END: List<Int> = listOf(1, 3, 6)
    val END_COUNTS: List<Int> = listOf(1, 3, 6, 9, 12)

    fun indexOfDistance(value: Int): Int = indexOf(DISTANCES_M, value)
    fun distanceAtIndex(index: Int): Int = valueAt(DISTANCES_M, index)

    fun indexOfEnds(value: Int): Int = indexOf(END_COUNTS, value)
    fun endCountAtIndex(index: Int): Int = valueAt(END_COUNTS, index)

    fun indexOfArrows(value: Int): Int = indexOf(ARROWS_PER_END, value)
    fun arrowsAtIndex(index: Int): Int = valueAt(ARROWS_PER_END, index)

    fun nearest(values: List<Int>, saved: Int): Int {
        require(values.isNotEmpty()) { "values must not be empty" }
        var best = values.first()
        var bestDistance = distance(best, saved)
        values.forEach { candidate ->
            val candidateDistance = distance(candidate, saved)
            if (candidateDistance < bestDistance ||
                (candidateDistance == bestDistance && candidate > best)
            ) {
                best = candidate
                bestDistance = candidateDistance
            }
        }
        return best
    }

    private fun indexOf(values: List<Int>, value: Int): Int {
        val exact = values.indexOf(value)
        return if (exact >= 0) exact else values.indexOf(nearest(values, value))
    }

    private fun valueAt(values: List<Int>, index: Int): Int = values[index.coerceIn(0, values.lastIndex)]

    private fun distance(value: Int, saved: Int): Long = abs(saved.toLong() - value.toLong())
}
