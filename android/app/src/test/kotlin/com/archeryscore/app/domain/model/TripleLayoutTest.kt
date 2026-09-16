package com.archeryscore.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TripleLayoutTest {

    @Test
    fun `documented triple spot centers match spec constants`() {
        assertEquals(1, TargetType.CM122.spotCount)
        assertEquals(listOf(0f to 0f), TargetType.CM122.spotCenters)

        assertEquals(3, TargetType.TRIPLE_VERTICAL.spotCount)
        assertEquals(
            listOf(0f to -2.2f, 0f to 0f, 0f to 2.2f),
            TargetType.TRIPLE_VERTICAL.spotCenters,
        )

        assertEquals(3, TargetType.TRIPLE_TRIANGULAR.spotCount)
        assertEquals(
            listOf(0f to -1.1f, -1.1f to 0.95f, 1.1f to 0.95f),
            TargetType.TRIPLE_TRIANGULAR.spotCenters,
        )
    }

    @Test
    fun `trip inside bottom spot of vertical face resolves to that spot`() {
        val result = PlacementScorer.place(0.1f, -2.1f, TargetType.TRIPLE_VERTICAL, RoundType.TEN_ZONE)
        assertEquals(false, result.miss)
        assertEquals(0, result.spotIndex)
    }

    @Test
    fun `trip inside middle spot of vertical face resolves to that spot`() {
        val result = PlacementScorer.place(-0.1f, 0f, TargetType.TRIPLE_VERTICAL, RoundType.TEN_ZONE)
        assertEquals(false, result.miss)
        assertEquals(1, result.spotIndex)
    }

    @Test
    fun `trip inside top spot of vertical face resolves to that spot`() {
        val result = PlacementScorer.place(0f, 2.1f, TargetType.TRIPLE_VERTICAL, RoundType.TEN_ZONE)
        assertEquals(false, result.miss)
        assertEquals(2, result.spotIndex)
    }

    @Test
    fun `tap between adjacent spots resolves to the nearest spot`() {
        // Gap between bottom (0,-2.2) and middle (0,0) spots of the vertical face.
        // Points in the gap are r ≥ 1 from both → miss, but resolution picks the nearer spot.
        val closerToMiddle = PlacementScorer.place(0f, -1.09f, TargetType.TRIPLE_VERTICAL, RoundType.TEN_ZONE)
        assertEquals(true, closerToMiddle.miss)
        assertEquals(1, closerToMiddle.spotIndex)

        val closerToBottom = PlacementScorer.place(0f, -1.11f, TargetType.TRIPLE_VERTICAL, RoundType.TEN_ZONE)
        assertEquals(true, closerToBottom.miss)
        assertEquals(0, closerToBottom.spotIndex)

        val equidistant = PlacementScorer.place(0f, -1.1f, TargetType.TRIPLE_VERTICAL, RoundType.TEN_ZONE)
        assertEquals(true, equidistant.miss)
    }

    @Test
    fun `tap outside all triple spots is a miss`() {
        val result = PlacementScorer.place(3f, 3f, TargetType.TRIPLE_TRIANGULAR, RoundType.TEN_ZONE)
        assertEquals(true, result.miss)
        assertEquals(0, result.score)
    }

    @Test
    fun `triple triangular returns the nearest of its three spots`() {
        val nearBottom = PlacementScorer.place(0f, -1.1f, TargetType.TRIPLE_TRIANGULAR, RoundType.TEN_ZONE)
        assertEquals(0, nearBottom.spotIndex)

        val nearLeft = PlacementScorer.place(-1.1f, 0.95f, TargetType.TRIPLE_TRIANGULAR, RoundType.TEN_ZONE)
        assertEquals(1, nearLeft.spotIndex)

        val nearRight = PlacementScorer.place(1.1f, 0.95f, TargetType.TRIPLE_TRIANGULAR, RoundType.TEN_ZONE)
        assertEquals(2, nearRight.spotIndex)
    }

    @Test
    fun `single face reports null spot index`() {
        val result = PlacementScorer.place(0f, 0f, TargetType.CM122, RoundType.TEN_ZONE)
        assertEquals(null, result.spotIndex)
    }
}