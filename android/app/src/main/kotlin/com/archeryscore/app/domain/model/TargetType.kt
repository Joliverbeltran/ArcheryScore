package com.archeryscore.app.domain.model

import androidx.annotation.StringRes
import com.archeryscore.app.R

enum class TargetType(
    @StringRes val labelRes: Int,
    val spotCount: Int,
    val spotCenters: List<Pair<Float, Float>>,
) {
    CM122(
        labelRes = R.string.target_type_122cm,
        spotCount = 1,
        spotCenters = listOf(0f to 0f),
    ),
    CM80(
        labelRes = R.string.target_type_80cm,
        spotCount = 1,
        spotCenters = listOf(0f to 0f),
    ),
    CM60(
        labelRes = R.string.target_type_60cm,
        spotCount = 1,
        spotCenters = listOf(0f to 0f),
    ),
    CM40(
        labelRes = R.string.target_type_40cm,
        spotCount = 1,
        spotCenters = listOf(0f to 0f),
    ),
    TRIPLE_VERTICAL(
        labelRes = R.string.target_type_triple_vertical,
        spotCount = 3,
        spotCenters = listOf(
            0f to -2.2f,
            0f to 0f,
            0f to 2.2f,
        ),
    ),
    TRIPLE_TRIANGULAR(
        labelRes = R.string.target_type_triple_triangular,
        spotCount = 3,
        spotCenters = listOf(
            0f to -1.1f,
            -1.1f to 0.95f,
            1.1f to 0.95f,
        ),
    ),
}
