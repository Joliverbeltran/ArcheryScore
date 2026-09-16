package com.archeryscore.app.ui.record

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.PlacementScorer
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.TargetType
import com.archeryscore.app.ui.components.FaceTransform
import com.archeryscore.app.ui.components.TargetFace
import com.archeryscore.app.ui.components.TargetMarker
import kotlin.math.abs

const val TARGET_TAG = "target_face"
const val OK_TAG = "placement_ok"
const val CANCEL_TAG = "placement_cancel"

@Composable
fun TargetPlacementDialog(
    targetType: TargetType,
    roundType: RoundType,
    arrowNumber: Int,
    arrowsPerEnd: Int,
    pending: PlacementPoint?,
    confirmedMarkers: List<PlacementPoint>,
    onPendingChange: (PlacementPoint) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isCorrection: Boolean = false,
    previousLabel: String? = null,
) {
    var transform by remember { mutableStateOf(FaceTransform(1f, 0f, 0f)) }
    var dragPoint by remember { mutableStateOf<PlacementPoint?>(null) }

    val extentX = targetType.spotCenters.maxOf { abs(it.first) } + 1f
    val extentY = targetType.spotCenters.maxOf { abs(it.second) } + 1f

    val marker = dragPoint ?: pending
    val provisional = pending?.let { point ->
        PlacementScorer.place(point.x, point.y, targetType, roundType)
    }

    val badgeText = when {
        provisional == null -> null
        provisional.miss -> stringResource(R.string.score_badge_miss)
        provisional.isXRing -> stringResource(R.string.score_badge_x)
        else -> stringResource(R.string.score_badge_ring_format, provisional.score)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isCorrection) {
                    stringResource(R.string.target_placement_correct_title, arrowNumber, arrowsPerEnd)
                } else {
                    stringResource(R.string.target_placement_title, arrowNumber)
                }
            )
        },
        text = {
            Column {
                if (isCorrection && previousLabel != null) {
                    Text(stringResource(R.string.target_placement_previous, previousLabel))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(extentX / extentY)
                            .testTag(TARGET_TAG)
                            .pointerInput(targetType) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    var normalized = clampToFace(
                                        transform.toNormalized(down.position),
                                        extentX,
                                        extentY,
                                    )
                                    dragPoint = PlacementPoint(normalized.first, normalized.second)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        change.consume()
                                        normalized = clampToFace(
                                            transform.toNormalized(change.position),
                                            extentX,
                                            extentY,
                                        )
                                        dragPoint = PlacementPoint(normalized.first, normalized.second)
                                    }
                                    onPendingChange(PlacementPoint(normalized.first, normalized.second))
                                    dragPoint = null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        TargetFace(
                            targetType = targetType,
                            modifier = Modifier.fillMaxSize(),
                            markers = buildList {
                                confirmedMarkers.forEach { add(TargetMarker(it.x, it.y)) }
                                marker?.let { add(TargetMarker(it.x, it.y)) }
                            },
                            contentDescription = buildString {
                                append(stringResource(R.string.target_face_description, stringResource(targetType.labelRes), scoringLabelRes(roundType)))
                                if (confirmedMarkers.isNotEmpty()) {
                                    append(". ")
                                    append(pluralStringResource(R.plurals.confirmed_arrows_count, confirmedMarkers.size, confirmedMarkers.size))
                                }
                            },
                            onTransform = { transform = it },
                        )
                    }
                }
                badgeText?.let { text ->
                    Surface(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .align(Alignment.CenterHorizontally)
                            .semantics { contentDescription = text },
                    ) {
                        Text(
                            text = text,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = pending != null,
                modifier = Modifier.testTag(OK_TAG),
            ) { Text(stringResource(R.string.target_placement_ok)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(CANCEL_TAG),
            ) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun clampToFace(
    point: Pair<Float, Float>,
    extentX: Float,
    extentY: Float,
): Pair<Float, Float> {
    fun clamp(value: Float, extent: Float): Float = value.coerceIn(-extent, extent)
    return clamp(point.first, extentX) to clamp(point.second, extentY)
}

private fun scoringLabelRes(roundType: RoundType): Int = when (roundType) {
    RoundType.TEN_ZONE -> R.string.ten_zone
    RoundType.FIVE_ZONE -> R.string.five_zone
}