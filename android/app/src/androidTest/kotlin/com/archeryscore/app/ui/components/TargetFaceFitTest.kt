package com.archeryscore.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.domain.model.TargetType
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TargetFaceFitTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every target type fits inside its container with uniform scale`() {
        val reported = mutableMapOf<TargetType, FaceTransform>()

        composeRule.setContent {
            Box(modifier = Modifier.size(320.dp)) {
                TargetType.entries.forEach { targetType ->
                    TargetFace(
                        targetType = targetType,
                        modifier = Modifier.size(280.dp),
                        onTransform = { transform -> reported[targetType] = transform },
                    )
                }
            }
        }

        composeRule.waitForIdle()

        TargetType.entries.forEach { targetType ->
            val transform = reported[targetType]
                ?: error("no transform reported for $targetType")

            val extentX = targetType.spotCenters.maxOf { abs(it.first) } + 1f
            val extentY = targetType.spotCenters.maxOf { abs(it.second) } + 1f
            val sizePx = with(composeRule.density) { 280.dp.toPx() }

            val expected = FaceTransform.fit(sizePx.toInt(), sizePx.toInt(), targetType)
            assertTrue(
                "uniform scale for $targetType: got ${transform.scale}, expected ${expected.scale}",
                abs(transform.scale - expected.scale) < 0.01f,
            )

            val leftEdge = transform.offsetX - extentX * transform.scale
            val rightEdge = transform.offsetX + extentX * transform.scale
            val topEdge = transform.offsetY - extentY * transform.scale
            val bottomEdge = transform.offsetY + extentY * transform.scale
            assertTrue("$targetType right edge $rightEdge exceeds width $sizePx", rightEdge <= sizePx + 0.01f)
            assertTrue("$targetType left edge $leftEdge below 0", leftEdge >= -0.01f)
            assertTrue("$targetType bottom edge $bottomEdge exceeds height $sizePx", bottomEdge <= sizePx + 0.01f)
            assertTrue("$targetType top edge $topEdge below 0", topEdge >= -0.01f)

            val minScale = min(sizePx / (2f * extentX), sizePx / (2f * extentY))
            assertTrue("$targetType scale ${transform.scale} overflows allowed $minScale", transform.scale <= minScale + 0.01f)
        }
    }

    @Test
    fun `small and rotated containers are handled without distortion`() {
        val landscape = mutableMapOf<TargetType, FaceTransform>()
        val portrait = mutableMapOf<TargetType, FaceTransform>()

        composeRule.setContent {
            TargetType.entries.filter { it.spotCount == 1 }.forEach { targetType ->
                Box(modifier = Modifier.size(300.dp, 120.dp)) {
                    TargetFace(
                        targetType = targetType,
                        modifier = Modifier.size(280.dp, 100.dp),
                        onTransform = { landscape[targetType] = it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.setContent {
            TargetType.entries.filter { it.spotCount == 1 }.forEach { targetType ->
                Box(modifier = Modifier.size(120.dp, 300.dp)) {
                    TargetFace(
                        targetType = targetType,
                        modifier = Modifier.size(100.dp, 280.dp),
                        onTransform = { portrait[targetType] = it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        TargetType.entries.filter { it.spotCount == 1 }.forEach { targetType ->
            val landscapeTransform = landscape[targetType] ?: error("no landscape transform for $targetType")
            val portraitTransform = portrait[targetType] ?: error("no portrait transform for $targetType")

            val ext = 1f
            val landscapeSize = with(composeRule.density) { min(280.dp.toPx(), 100.dp.toPx()) }
            val portraitSize = with(composeRule.density) { min(100.dp.toPx(), 280.dp.toPx()) }

            assertTrue(
                "$targetType landscape scale ${landscapeTransform.scale} does not fill short axis",
                abs(landscapeTransform.scale - landscapeSize / (2f * ext)) < 0.01f,
            )
            assertTrue(
                "$targetType portrait scale ${portraitTransform.scale} does not fill short axis",
                abs(portraitTransform.scale - portraitSize / (2f * ext)) < 0.01f,
            )
        }
    }
}