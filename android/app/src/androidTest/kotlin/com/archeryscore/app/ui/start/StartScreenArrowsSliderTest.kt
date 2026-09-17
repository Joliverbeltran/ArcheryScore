package com.archeryscore.app.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.SessionSetupOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartScreenArrowsSliderTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun ArrowsSlider(savedDefault: Int) {
        var index by remember {
            mutableIntStateOf(
                SessionSetupOptions.indexOfArrows(
                    SessionSetupOptions.nearest(SessionSetupOptions.ARROWS_PER_END, savedDefault),
                ),
            )
        }
        val value = SessionSetupOptions.arrowsAtIndex(index)
        OptionSlider(
            labelRes = R.string.arrows_per_end,
            values = SessionSetupOptions.ARROWS_PER_END,
            selectedIndex = index,
            onIndexChange = { index = it },
            valueText = pluralStringResource(R.plurals.arrows_per_end_value, value, value),
            testTag = ARROWS_PER_END_SLIDER_TAG,
        )
    }

    private fun label(arrows: Int) =
        if (arrows == 1) "$arrows arrow per end" else "$arrows arrows per end"

    @Test
    fun showsTheArrowsPerEndFieldLabel() {
        compose.setContent { ArrowsSlider(savedDefault = 3) }

        compose.onNodeWithText("Arrows per end").assertIsDisplayed()
    }

    @Test
    fun defaultOfThreeArrowsIsShown() {
        compose.setContent { ArrowsSlider(savedDefault = 3) }

        compose.onNodeWithText("3 arrows per end").assertIsDisplayed()
    }

    @Test
    fun storedDefaultThatIsNotAStepSnapsToTheNearestOption() {
        compose.setContent { ArrowsSlider(savedDefault = 4) }

        compose.onNodeWithText("3 arrows per end").assertIsDisplayed()
    }

    @Test
    fun everyArrowsOptionCanBeSelected() {
        compose.setContent { ArrowsSlider(savedDefault = 3) }

        SessionSetupOptions.ARROWS_PER_END.forEachIndexed { index, arrows ->
            compose.onNodeWithTag(ARROWS_PER_END_SLIDER_TAG)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(index.toFloat()) }
            compose.onNodeWithText(label(arrows)).assertIsDisplayed()
        }
    }
}
