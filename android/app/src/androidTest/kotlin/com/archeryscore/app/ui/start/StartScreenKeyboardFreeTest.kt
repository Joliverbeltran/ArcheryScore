package com.archeryscore.app.ui.start

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.SessionSetupOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartScreenKeyboardFreeTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun SetupSliders() {
        Column {
            var distanceIndex by remember { mutableIntStateOf(2) }
            var endsIndex by remember { mutableIntStateOf(2) }
            var arrowsIndex by remember { mutableIntStateOf(1) }
            OptionSlider(
                labelRes = R.string.distance_m,
                values = SessionSetupOptions.DISTANCES_M,
                selectedIndex = distanceIndex,
                onIndexChange = { distanceIndex = it },
                valueText = stringResource(
                    R.string.distance_value,
                    SessionSetupOptions.distanceAtIndex(distanceIndex),
                ),
                testTag = DISTANCE_SLIDER_TAG,
            )
            OptionSlider(
                labelRes = R.string.end_count,
                values = SessionSetupOptions.END_COUNTS,
                selectedIndex = endsIndex,
                onIndexChange = { endsIndex = it },
                valueText = pluralStringResource(
                    R.plurals.end_count_value,
                    SessionSetupOptions.endCountAtIndex(endsIndex),
                    SessionSetupOptions.endCountAtIndex(endsIndex),
                ),
                testTag = END_COUNT_SLIDER_TAG,
            )
            OptionSlider(
                labelRes = R.string.arrows_per_end,
                values = SessionSetupOptions.ARROWS_PER_END,
                selectedIndex = arrowsIndex,
                onIndexChange = { arrowsIndex = it },
                valueText = pluralStringResource(
                    R.plurals.arrows_per_end_value,
                    SessionSetupOptions.arrowsAtIndex(arrowsIndex),
                    SessionSetupOptions.arrowsAtIndex(arrowsIndex),
                ),
                testTag = ARROWS_PER_END_SLIDER_TAG,
            )
        }
    }

    @Test
    fun allThreeSetupValuesAreSlidersNotTextFields() {
        compose.setContent { SetupSliders() }

        listOf(DISTANCE_SLIDER_TAG, END_COUNT_SLIDER_TAG, ARROWS_PER_END_SLIDER_TAG).forEach { tag ->
            compose.onNodeWithTag(tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            compose.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
        }
    }

    @Test
    fun setupSectionContainsNoEditableTextInputs() {
        compose.setContent { SetupSliders() }

        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText)).assertCountEquals(0)
    }

    @Test
    fun labelsForAllThreeControlsAreVisible() {
        compose.setContent { SetupSliders() }

        compose.onNodeWithText("Distance (m)").assertIsDisplayed()
        compose.onNodeWithText("Ends").assertIsDisplayed()
        compose.onNodeWithText("Arrows per end").assertIsDisplayed()
    }
}
