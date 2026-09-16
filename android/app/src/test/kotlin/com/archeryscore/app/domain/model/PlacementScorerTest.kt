package com.archeryscore.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlacementScorerTest {

    private fun place(r: Float, targetType: TargetType = TargetType.CM122, scoringType: RoundType = RoundType.TEN_ZONE): PlacementResult =
        PlacementScorer.place(x = r, y = 0f, targetType = targetType, scoringType = scoringType)

    @Test
    fun `TEN_ZONE scores every ring value`() {
        assertEquals(10, place(0.00f).score)
        assertEquals(10, place(0.05f).score)
        assertEquals(9, place(0.15f).score)
        assertEquals(8, place(0.25f).score)
        assertEquals(7, place(0.35f).score)
        assertEquals(6, place(0.45f).score)
        assertEquals(5, place(0.55f).score)
        assertEquals(4, place(0.65f).score)
        assertEquals(3, place(0.75f).score)
        assertEquals(2, place(0.85f).score)
        assertEquals(1, place(0.95f).score)
    }

    @Test
    fun `FIVE_ZONE maps bands 1 to 5 outer to inner`() {
        assertEquals(5, place(0.10f, scoringType = RoundType.FIVE_ZONE).score)
        assertEquals(4, place(0.30f, scoringType = RoundType.FIVE_ZONE).score)
        assertEquals(3, place(0.50f, scoringType = RoundType.FIVE_ZONE).score)
        assertEquals(2, place(0.70f, scoringType = RoundType.FIVE_ZONE).score)
        assertEquals(1, place(0.90f, scoringType = RoundType.FIVE_ZONE).score)
    }

    @Test
    fun `boundary touch scores the higher inner value for TEN_ZONE`() {
        val w = 1f / RoundType.TEN_ZONE.maxScore
        (1..9).forEach { k ->
            val r = k * w
            val expected = RoundType.TEN_ZONE.maxScore + 1 - k
            assertEquals(expected, place(r).score, "boundary at ${k}w (=r=$r) should score the higher value $expected")
        }
    }

    @Test
    fun `boundary touch scores the higher inner value for FIVE_ZONE`() {
        val w = 1f / RoundType.FIVE_ZONE.maxScore
        (1..4).forEach { k ->
            val r = k * w
            val expected = RoundType.FIVE_ZONE.maxScore + 1 - k
            assertEquals(expected, place(r, scoringType = RoundType.FIVE_ZONE).score, "band boundary at r=$r should score $expected")
        }
    }

    @Test
    fun `r of 1 or more is a miss`() {
        listOf(1.00f, 1.01f, 1.10f, 1.5f, 2.0f, 5.0f).forEach { r ->
            val result = place(r)
            assertTrue(result.miss, "r=$r should be a miss")
            assertEquals(0, result.score, "r=$r should score 0")
            assertFalse(result.isXRing)
        }
        assertTrue(place(1.0f, scoringType = RoundType.FIVE_ZONE).miss)
        assertEquals(0, place(1.0f, scoringType = RoundType.FIVE_ZONE).score)
    }

    @Test
    fun `X only when TEN_ZONE and r at most half a zone width`() {
        val w = 1f / RoundType.TEN_ZONE.maxScore
        assertTrue(place(0f).isXRing)
        assertTrue(place(0.5f * w).isXRing)
        assertFalse(place(0.5f * w + 0.01f).isXRing)
        assertEquals(10, place(0.5f * w).score)
        assertEquals(10, place(0.5f * w + 0.01f).score)
    }

    @Test
    fun `FIVE_ZONE isX is always false`() {
        listOf(0f, 0.05f, 0.19f, 0.2f, 0.5f, 0.9f, 0.99f).forEach { r ->
            val result = place(r, scoringType = RoundType.FIVE_ZONE)
            assertTrue(result.score in 1..5, "r=$r should score within 1..5")
            assertFalse(result.isXRing, "FIVE_ZONE must never yield X at r=$r")
        }
    }

    @Test
    fun `FIVE_ZONE still scores the centre band`() {
        val result = place(0f, scoringType = RoundType.FIVE_ZONE)
        assertEquals(5, result.score)
        assertFalse(result.isXRing)
        assertFalse(result.miss)
    }

    @Test
    fun `triple vertical resolves nearest spot centre and scores relative to it`() {
        val target = TargetType.TRIPLE_VERTICAL
        val top = PlacementScorer.place(x = -0.1f, y = 2.2f, targetType = target, scoringType = RoundType.TEN_ZONE)
        assertEquals(2, top.spotIndex)
        assertEquals(10, top.score)

        val bottom = PlacementScorer.place(x = 0.15f, y = -2.2f, targetType = target, scoringType = RoundType.TEN_ZONE)
        assertEquals(0, bottom.spotIndex)
        assertEquals(9, bottom.score)

        val centre = PlacementScorer.place(x = 0.05f, y = 0f, targetType = target, scoringType = RoundType.TEN_ZONE)
        assertEquals(1, centre.spotIndex)
        assertEquals(10, centre.score)
    }

    @Test
    fun `triple gap between spots resolves to nearest spot and may miss`() {
        val target = TargetType.TRIPLE_VERTICAL
        val midpoint = PlacementScorer.place(x = 0f, y = 1.1f, targetType = target, scoringType = RoundType.TEN_ZONE)
        assertEquals(1, midpoint.spotIndex)
        assertTrue(midpoint.miss)
        assertEquals(0, midpoint.score)
    }

    @Test
    fun `single faces expose null spotIndex`() {
        assertNull(place(0f).spotIndex)
    }

    @Test
    fun `exhaustive TEN_ZONE boundary matrix scores the higher value at every ring boundary`() {
        val w = 1f / RoundType.TEN_ZONE.maxScore
        (1 until RoundType.TEN_ZONE.maxScore).forEach { k ->
            val r = k * w
            val expected = RoundType.TEN_ZONE.maxScore + 1 - k
            val epsilon = 0.0001f
            assertEquals(expected, place(r).score, "exact boundary r=$r")
            assertEquals(expected, place(r - epsilon).score, "just inside boundary r=$r")
            assertEquals(expected - 1, place(r + epsilon).score, "just outside boundary r=$r")
        }
    }

    @Test
    fun `exhaustive FIVE_ZONE boundary matrix scores the higher value at every band boundary`() {
        val w = 1f / RoundType.FIVE_ZONE.maxScore
        (1 until RoundType.FIVE_ZONE.maxScore).forEach { k ->
            val r = k * w
            val expected = RoundType.FIVE_ZONE.maxScore + 1 - k
            val epsilon = 0.0001f
            assertEquals(expected, place(r, scoringType = RoundType.FIVE_ZONE).score, "exact boundary r=$r")
            assertEquals(expected, place(r - epsilon, scoringType = RoundType.FIVE_ZONE).score, "just inside boundary r=$r")
            assertEquals(expected - 1, place(r + epsilon, scoringType = RoundType.FIVE_ZONE).score, "just outside boundary r=$r")
        }
    }

    @Test
    fun `X rule re-verified across the full ring width for TEN_ZONE`() {
        val w = 1f / RoundType.TEN_ZONE.maxScore
        (0..99).map { i -> i / 100f * w }.forEach { r ->
            val result = place(r)
            assertEquals(10, result.score, "inner band r=$r")
            assertEquals(r <= 0.5f * w, result.isXRing, "X flag at r=$r")
        }
    }

    @Test
    fun `boundary and miss behavior is identical across all six target types`() {
        val w = 1f / RoundType.TEN_ZONE.maxScore
        val epsilon = 0.0001f
        TargetType.entries.forEach { target ->
            // Center of single faces and the vertical triple's middle spot scores; the
            // triangular triple has no spot at (0,0), so its center may be a miss.
            val center = PlacementScorer.place(0f, 0f, target, RoundType.TEN_ZONE)
            if (target.spotCenters.any { it.first == 0f && it.second == 0f }) {
                assertFalse(center.miss, "$target center must not be a miss")
                assertEquals(10, center.score, "$target center score ${center.score}")
            }

            // Far outside always misses.
            val far = PlacementScorer.place(5f, 5f, target, RoundType.TEN_ZONE)
            assertTrue(far.miss, "$target far point must be a miss")

            // Single-face ring math identical across the four CM faces.
            if (target.spotCount == 1) {
                assertEquals(10, PlacementScorer.place(w - epsilon, 0f, target, RoundType.TEN_ZONE).score, "$target inner band")
                assertEquals(1, PlacementScorer.place(0.95f, 0f, target, RoundType.TEN_ZONE).score, "$target outer band")
            }
        }
    }
}