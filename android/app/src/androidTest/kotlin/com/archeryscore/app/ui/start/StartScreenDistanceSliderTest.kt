package com.archeryscore.app.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.res.stringResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.SessionSetupOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartScreenDistanceSliderTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun DistanceSlider(savedDefault: Int) {
        var index by remember {
            mutableIntStateOf(
                SessionSetupOptions.indexOfDistance(
                    SessionSetupOptions.nearest(SessionSetupOptions.DISTANCES_M, savedDefault),
                ),
            )
        }
        OptionSlider(
            labelRes = R.string.distance_m,
            values = SessionSetupOptions.DISTANCES_M,
            selectedIndex = index,
            onIndexChange = { index = it },
            valueText = stringResource(R.string.distance_value, SessionSetupOptions.distanceAtIndex(index)),
            testTag = DISTANCE_SLIDER_TAG,
        )
    }

    @Test
    fun showsTheDistanceFieldLabel() {
        compose.setContent { DistanceSlider(savedDefault = 18) }

        compose.onNodeWithText("Distance (m)").assertIsDisplayed()
    }

    @Test
    fun defaultDistanceOfEighteenMetersIsShown() {
        compose.setContent { DistanceSlider(savedDefault = 18) }

        compose.onNodeWithText("18 m").assertIsDisplayed()
    }

    @Test
    fun storedDefaultThatIsNotAStepSnapsToTheNearestOption() {
        compose.setContent { DistanceSlider(savedDefault = 25) }

        compose.onNodeWithText("30 m").assertIsDisplayed()
    }

    @Test
    fun everyDistanceOptionCanBeSelected() {
        compose.setContent { DistanceSlider(savedDefault = 18) }

        SessionSetupOptions.DISTANCES_M.forEachIndexed { index, meters ->
            compose.onNodeWithTag(DISTANCE_SLIDER_TAG)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(index.toFloat()) }
            compose.onNodeWithText("$meters m").assertIsDisplayed()
        }
    }
}
