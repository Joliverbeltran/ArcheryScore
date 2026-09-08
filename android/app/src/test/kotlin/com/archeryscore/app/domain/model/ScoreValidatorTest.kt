package com.archeryscore.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ScoreValidatorTest {

    @Test
    fun `ten zone accepts scores 1 to 10`() {
        for (score in 1..10) {
            assertTrue(ScoreValidator.isValid(score, RoundType.TEN_ZONE))
        }
    }

    @Test
    fun `five zone accepts scores 1 to 5`() {
        for (score in 1..5) {
            assertTrue(ScoreValidator.isValid(score, RoundType.FIVE_ZONE))
        }
    }

    @Test
    fun `ten zone rejects score above max`() {
        assertFalse(ScoreValidator.isValid(11, RoundType.TEN_ZONE))
    }

    @Test
    fun `five zone rejects score above max`() {
        assertFalse(ScoreValidator.isValid(6, RoundType.FIVE_ZONE))
    }

    @Test
    fun `negative scores are rejected for all round types`() {
        assertFalse(ScoreValidator.isValid(-1, RoundType.TEN_ZONE))
        assertFalse(ScoreValidator.isValid(-5, RoundType.FIVE_ZONE))
    }

    @Test
    fun `zero score is rejected`() {
        assertFalse(ScoreValidator.isValid(0, RoundType.TEN_ZONE))
    }

    @Test
    fun `x ring requires score of 10`() {
        assertTrue(ScoreValidator.canBeXRing(10, RoundType.TEN_ZONE))
        assertFalse(ScoreValidator.canBeXRing(9, RoundType.TEN_ZONE))
    }

    @Test
    fun `x ring is never allowed in five zone`() {
        assertFalse(ScoreValidator.canBeXRing(5, RoundType.FIVE_ZONE))
    }

    @Test
    fun `assertValid throws on invalid score`() {
        assertThrows<IllegalArgumentException> {
            ScoreValidator.assertValid(11, RoundType.TEN_ZONE)
        }
    }

    @Test
    fun `assertValid throws when xring set on non ten`() {
        assertThrows<IllegalArgumentException> {
            ScoreValidator.assertValid(score = 8, roundType = RoundType.TEN_ZONE, isXRing = true)
        }
    }

    @Test
    fun `assertValid accepts valid xring arrow`() {
        assertEquals(10, ScoreValidator.assertValid(10, RoundType.TEN_ZONE, isXRing = true))
    }
}