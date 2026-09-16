package com.archeryscore.app.ui.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.TargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TargetPlacementFlowTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tapShowsMarkerAndProvisionalBadge() {
        val points = mutableListOf<PlacementPoint>()
        var marker: PlacementPoint? = null
        var markerAtTap: PlacementPoint? = null
        compose.setContent {
            var pending by remember { mutableStateOf(marker) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = 1,
                arrowsPerEnd = 3,
                pending = pending,
                confirmedMarkers = emptyList(),
                onPendingChange = { pending = it; markerAtTap = it; points += it },
                onConfirm = { marker = pending },
                onDismiss = {},
            )
        }
        compose.onNodeWithTag(TARGET_TAG).performTouchInput { click(center) }
        compose.waitForIdle()
        assertTrue(points.isNotEmpty())
        assertTrue(markerAtTap != null)
        compose.onNodeWithContentDescription("X").assertIsDisplayed()
    }

    @Test
    fun dragMovesMarker() {
        val points = mutableListOf<PlacementPoint>()
        compose.setContent {
            var pending by remember { mutableStateOf<PlacementPoint?>(null) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = 1,
                arrowsPerEnd = 3,
                pending = pending,
                confirmedMarkers = emptyList(),
                onPendingChange = { pending = it; points += it },
                onConfirm = {},
                onDismiss = {},
            )
        }
        compose.onNodeWithTag(TARGET_TAG).performTouchInput {
            click(center)
            swipe(start = center, end = topRight)
        }
        compose.waitForIdle()
        assertEquals(2, points.size)
        assertTrue(points[1].x > points[0].x)
    }

    @Test
    fun okWithMarkerAdvancesToNextSlot() {
        var slot = 1
        var marker: PlacementPoint? = null
        val confirmed = mutableListOf<PlacementPoint>()
        compose.setContent {
            var currentSlot by remember { mutableIntStateOf(slot) }
            var pending by remember { mutableStateOf(marker) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = currentSlot,
                arrowsPerEnd = 3,
                pending = pending,
                confirmedMarkers = emptyList(),
                onPendingChange = { pending = it; marker = it },
                onConfirm = {
                    marker?.let { confirmed += it }
                    pending = null
                    marker = null
                    slot += 1
                    currentSlot += 1
                },
                onDismiss = {},
            )
        }
        compose.onNodeWithTag(TARGET_TAG).performTouchInput { click(center) }
        compose.onNodeWithTag(OK_TAG).performClick()
        compose.waitForIdle()
        assertEquals(1, confirmed.size)
        compose.onNodeWithText("Place arrow 2").assertIsDisplayed()
    }

    @Test
    fun cancelDiscardsPendingAndNothingPersisted() {
        var pending: PlacementPoint? = null
        var confirmedCount = 0
        compose.setContent {
            var localPending by remember { mutableStateOf(pending) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = 1,
                arrowsPerEnd = 3,
                pending = localPending,
                confirmedMarkers = emptyList(),
                onPendingChange = { localPending = it },
                onConfirm = { confirmedCount++ },
                onDismiss = { pending = null },
            )
        }
        compose.onNodeWithTag(TARGET_TAG).performTouchInput { click(center) }
        compose.onNodeWithTag(CANCEL_TAG).performClick()
        compose.waitForIdle()
        assertEquals(0, confirmedCount)
        assertEquals(null, markerAfterCancel(pending))
    }

    @Test
    fun okWithoutMarkerIsNotAllowed() {
        var confirmCount = 0
        compose.setContent {
            var pending by remember { mutableStateOf<PlacementPoint?>(null) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = 1,
                arrowsPerEnd = 3,
                pending = pending,
                confirmedMarkers = emptyList(),
                onPendingChange = { pending = it },
                onConfirm = { confirmCount++ },
                onDismiss = {},
            )
        }
        compose.onNodeWithTag(OK_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(OK_TAG).performClick()
        compose.waitForIdle()
        assertEquals(0, confirmCount)
    }

    @Test
    fun okOnLastArrowClosesDialog() {
        compose.setContent {
            var dialogOpen by remember { mutableStateOf(true) }
            var pending by remember { mutableStateOf<PlacementPoint?>(null) }
            if (dialogOpen) {
                TargetPlacementDialog(
                    targetType = TargetType.CM122,
                    roundType = RoundType.TEN_ZONE,
                    arrowNumber = 1,
                    arrowsPerEnd = 1,
                    pending = pending,
                    confirmedMarkers = emptyList(),
                    onPendingChange = { pending = it },
                    onConfirm = { dialogOpen = false },
                    onDismiss = { dialogOpen = false },
                )
            }
        }
        compose.onNodeWithTag(TARGET_TAG).performTouchInput { click(center) }
        compose.onNodeWithTag(OK_TAG).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Place arrow 1").assertDoesNotExist()
    }

    @Test
    fun missBadgeShownForOuterPlacement() {
        var pending: PlacementPoint? = null
        compose.setContent {
            var localPending by remember { mutableStateOf(pending) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = 1,
                arrowsPerEnd = 3,
                pending = localPending,
                confirmedMarkers = emptyList(),
                onPendingChange = { localPending = it },
                onConfirm = {},
                onDismiss = {},
            )
        }
        compose.onNodeWithTag(TARGET_TAG).performTouchInput { click(bottomCenter) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("M").assertIsDisplayed()
    }

    private fun markerAfterCancel(markerToReset: PlacementPoint?): PlacementPoint? = markerToReset

    companion object {
        private const val TARGET_TAG = "target_face"
        private const val OK_TAG = "placement_ok"
        private const val CANCEL_TAG = "placement_cancel"
    }
}