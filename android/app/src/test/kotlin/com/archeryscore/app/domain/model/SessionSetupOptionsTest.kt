package com.archeryscore.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionSetupOptionsTest {

    private val distances = SessionSetupOptions.DISTANCES_M
    private val arrows = SessionSetupOptions.ARROWS_PER_END
    private val ends = SessionSetupOptions.END_COUNTS

    @Test
    fun `distance options are the eight standard tournament distances`() {
        assertEquals(listOf(8, 12, 18, 30, 40, 50, 70, 90), distances)
    }

    @Test
    fun `arrows per end options are one three and six`() {
        assertEquals(listOf(1, 3, 6), arrows)
    }

    @Test
    fun `end count options are one three six nine twelve`() {
        assertEquals(listOf(1, 3, 6, 9, 12), ends)
    }

    @Test
    fun `every option list is non-empty strictly ascending and unique`() {
        listOf(distances, arrows, ends).forEach { list ->
            assertTrue(list.isNotEmpty())
            assertEquals(list.sorted(), list, "list must be ascending: $list")
            assertEquals(list.distinct(), list, "list must be unique: $list")
        }
    }

    @Test
    fun `index and value round trip for every option`() {
        distances.forEach { v ->
            assertEquals(v, SessionSetupOptions.distanceAtIndex(SessionSetupOptions.indexOfDistance(v)))
        }
        ends.forEach { v ->
            assertEquals(v, SessionSetupOptions.endCountAtIndex(SessionSetupOptions.indexOfEnds(v)))
        }
        arrows.forEach { v ->
            assertEquals(v, SessionSetupOptions.arrowsAtIndex(SessionSetupOptions.indexOfArrows(v)))
        }
    }

    @Test
    fun `nearest returns the exact element when the saved value is a valid option`() {
        listOf(distances, arrows, ends).forEach { list ->
            list.forEach { value ->
                assertEquals(value, SessionSetupOptions.nearest(list, value))
            }
        }
    }

    @Test
    fun `nearest picks the closest distance`() {
        assertEquals(30, SessionSetupOptions.nearest(distances, 25))
        assertEquals(18, SessionSetupOptions.nearest(distances, 20))
    }

    @Test
    fun `nearest clamps values outside the range`() {
        assertEquals(8, SessionSetupOptions.nearest(distances, 0))
        assertEquals(90, SessionSetupOptions.nearest(distances, 100))
        assertEquals(8, SessionSetupOptions.nearest(distances, Int.MIN_VALUE))
        assertEquals(90, SessionSetupOptions.nearest(distances, Int.MAX_VALUE))
    }

    @Test
    fun `nearest rounds up when two options are equidistant`() {
        assertEquals(6, SessionSetupOptions.nearest(ends, 5))
        assertEquals(3, SessionSetupOptions.nearest(arrows, 2))
    }

    @Test
    fun `nearest end count reference vectors`() {
        assertEquals(3, SessionSetupOptions.nearest(ends, 4))
        assertEquals(6, SessionSetupOptions.nearest(ends, 7))
        assertEquals(12, SessionSetupOptions.nearest(ends, 12))
    }

    @Test
    fun `indexOf snaps a non option value to its nearest option`() {
        assertEquals(SessionSetupOptions.indexOfDistance(30), SessionSetupOptions.indexOfDistance(25))
        assertEquals(SessionSetupOptions.indexOfArrows(3), SessionSetupOptions.indexOfArrows(4))
    }

    @Test
    fun `valueAt coerces out of range indices`() {
        assertEquals(8, SessionSetupOptions.distanceAtIndex(-5))
        assertEquals(90, SessionSetupOptions.distanceAtIndex(99))
        assertEquals(1, SessionSetupOptions.endCountAtIndex(-1))
        assertEquals(12, SessionSetupOptions.endCountAtIndex(42))
    }
}
