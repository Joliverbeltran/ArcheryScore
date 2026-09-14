package com.archeryscore.app.domain.model

data class ImportedSession(
    val session: Session,
    val ends: List<ImportedEnd>,
)

data class ImportedEnd(
    val endNumber: Int,
    val arrows: List<ImportedArrow>,
)

data class ImportedArrow(
    val arrowNumber: Int,
    val score: Int,
    val isXRing: Boolean,
)