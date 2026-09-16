package com.archeryscore.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.archeryscore.app.domain.model.TargetType
import kotlin.math.abs
import kotlin.math.min

data class TargetMarker(
    val x: Float,
    val y: Float,
)

data class FaceTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    fun toScreen(x: Float, y: Float): Offset = Offset(offsetX + x * scale, offsetY + y * scale)

    fun toNormalized(offset: Offset): Pair<Float, Float> =
        Pair((offset.x - offsetX) / scale, (offset.y - offsetY) / scale)

    companion object {
        fun fit(width: Int, height: Int, targetType: TargetType): FaceTransform {
            val extentX = targetType.spotCenters.maxOf { abs(it.first) } + 1f
            val extentY = targetType.spotCenters.maxOf { abs(it.second) } + 1f
            val scale = min(width / (2f * extentX), height / (2f * extentY))
            return FaceTransform(scale = scale, offsetX = width / 2f, offsetY = height / 2f)
        }
    }
}

@Composable
fun TargetFace(
    targetType: TargetType,
    modifier: Modifier = Modifier,
    markers: List<TargetMarker> = emptyList(),
    contentDescription: String? = null,
    onTransform: (FaceTransform) -> Unit = {},
) {
    var transform by remember { mutableStateOf(FaceTransform(1f, 0f, 0f)) }
    val markerRadiusPx = with(LocalDensity.current) { 6.dp.toPx() }

    Box(
        modifier = modifier
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .onSizeChanged { size ->
                transform = FaceTransform.fit(size.width, size.height, targetType)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawFace(targetType, transform)
            markers.forEach { marker ->
                val px = transform.toScreen(marker.x, marker.y)
                drawCircle(
                    color = Color(0xFF1B1B1B),
                    radius = markerRadiusPx,
                    center = px,
                )
                drawCircle(
                    color = Color(0xFFF5F5F5),
                    radius = markerRadiusPx * 0.6f,
                    center = px,
                )
            }
        }
    }

    LaunchedEffect(transform) {
        onTransform(transform)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFace(
    targetType: TargetType,
    transform: FaceTransform,
) {
    val w = 1f / 10f
    val spotCenters = targetType.spotCenters
    spotCenters.forEach { (cx, cy) ->
        drawSpot(cx, cy, transform, w)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpot(
    centerX: Float,
    centerY: Float,
    transform: FaceTransform,
    w: Float,
) {
    val center = transform.toScreen(centerX, centerY)

    // Colored bands, outer (value 1) to inner (value 10), so inner rings stack on top.
    for (v in 1..10) {
        val radius = (11 - v) * w
        drawCircle(
            color = bandColor(v),
            radius = radius * transform.scale,
            center = center,
        )
    }

    // Thin dividing lines split each colour pair (10/9, 8/7, 6/5, 4/3, 2/1).
    drawScopeLine(center, transform, w, lineAt = 1f)
    drawScopeLine(center, transform, w, lineAt = 3f)
    drawScopeLine(center, transform, w, lineAt = 5f)
    drawScopeLine(center, transform, w, lineAt = 7f)
    drawScopeLine(center, transform, w, lineAt = 9f)

    // Inner-10 "X" ring.
    drawCircle(
        color = Color(0x80101010),
        radius = 0.5f * w * transform.scale,
        center = center,
        style = Stroke(width = LineWidth),
    )

    // Scoring-face outline.
    drawCircle(
        color = Color(0x80202020),
        radius = transform.scale,
        center = center,
        style = Stroke(width = LineWidth),
    )
}

private const val LineWidth = 2f

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScopeLine(
    center: Offset,
    transform: FaceTransform,
    w: Float,
    lineAt: Float,
) {
    val radius = lineAt * w * transform.scale
    drawCircle(
        color = Color(0xB3101010),
        radius = radius,
        center = center,
        style = Stroke(width = LineWidth),
    )
}

private fun bandColor(value: Int): Color = when (value) {
    10, 9 -> Color(0xFFEFDF00)
    8, 7 -> Color(0xFFEE1C25)
    6, 5 -> Color(0xFF00A1E8)
    4, 3 -> Color(0xFF1B1B1B)
    else -> Color(0xFFF7F7F7)
}