package com.archeryscore.app.ui.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.TargetType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TargetMarkersVisibleTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previouslyConfirmedMarkersStayVisibleDuringNextPlacement() {
        val confirmed: MutableList<PlacementPoint> = mutableListOf(
            PlacementPoint(0.2f, -0.1f),
            PlacementPoint(-0.15f, 0.3f),
        )
        var pending: PlacementPoint? = null
        var onPendingChanged = false
        compose.setContent {
            var localPending by remember { mutableStateOf(pending) }
            TargetPlacementDialog(
                targetType = TargetType.CM122,
                roundType = RoundType.TEN_ZONE,
                arrowNumber = 3,
                arrowsPerEnd = 3,
                pending = localPending,
                confirmedMarkers = confirmed,
                onPendingChange = { localPending = it; onPendingChanged = true },
                onConfirm = {},
                onDismiss = {},
            )
        }
        compose.waitForIdle()

        // The two confirmed arrows of the end remain visible while placing arrow 3.
        compose.onNodeWithContentDescription("2 confirmed arrows").assertIsDisplayed()

        // Placing the next arrow keeps the confirmed markers untouched.
        compose.onNodeWithTag(TARGET_TAG).performTouchInput { click(center) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("2 confirmed arrows").assertIsDisplayed()
        assertTrue(onPendingChanged)
    }

    companion object {
        private const val TARGET_TAG = "target_face"
    }
}