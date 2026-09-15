package com.archeryscore.app.domain.model

object ScoreValidator {

    fun isValid(score: Int, roundType: RoundType): Boolean = score in 1..roundType.maxScore

    fun canBeXRing(score: Int, roundType: RoundType): Boolean =
        roundType == RoundType.TEN_ZONE && score == 10

    fun assertValid(score: Int, roundType: RoundType, isXRing: Boolean = false): Int {
        require(isValid(score, roundType)) {
            "Score $score is out of range for ${roundType.name} (1..${roundType.maxScore})"
        }
        require(!isXRing || canBeXRing(score, roundType)) {
            "X-ring is only allowed for a score of 10 in TEN_ZONE"
        }
        return score
    }
}