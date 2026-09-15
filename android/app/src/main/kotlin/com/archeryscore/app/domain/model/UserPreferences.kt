package com.archeryscore.app.domain.model

data class UserPreferences(
    val defaultRoundType: RoundType = RoundType.TEN_ZONE,
    val defaultEndCount: Int = 6,
    val defaultArrowsPerEnd: Int = 3,
    val defaultDistanceM: Int = 18,
    val defaultDiscipline: Discipline = Discipline.OLYMPIC_RECURVE,
    val countXRingsDeeply: Boolean = false,
)