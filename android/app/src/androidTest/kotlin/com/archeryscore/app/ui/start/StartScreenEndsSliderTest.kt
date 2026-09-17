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
class StartScreenEndsSliderTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun EndsSlider(savedDefault: Int) {
        var index by remember {
            mutableIntStateOf(
                SessionSetupOptions.indexOfEnds(
                    SessionSetupOptions.nearest(SessionSetupOptions.END_COUNTS, savedDefault),
                ),
            )
        }
        val value = SessionSetupOptions.endCountAtIndex(index)
        OptionSlider(
            labelRes = R.string.end_count,
            values = SessionSetupOptions.END_COUNTS,
            selectedIndex = index,
            onIndexChange = { index = it },
            valueText = pluralStringResource(R.plurals.end_count_value, value, value),
            testTag = END_COUNT_SLIDER_TAG,
        )
    }

    private fun label(ends: Int) = if (ends == 1) "$ends end" else "$ends ends"

    @Test
    fun showsTheEndsFieldLabel() {
        compose.setContent { EndsSlider(savedDefault = 6) }

        compose.onNodeWithText("Ends").assertIsDisplayed()
    }

    @Test
    fun defaultOfSixEndsIsShown() {
        compose.setContent { EndsSlider(savedDefault = 6) }

        compose.onNodeWithText("6 ends").assertIsDisplayed()
    }

    @Test
    fun storedDefaultThatIsNotAStepSnapsToTheNearestOption() {
        compose.setContent { EndsSlider(savedDefault = 5) }

        compose.onNodeWithText("6 ends").assertIsDisplayed()
    }

    @Test
    fun everyEndCountOptionCanBeSelected() {
        compose.setContent { EndsSlider(savedDefault = 6) }

        SessionSetupOptions.END_COUNTS.forEachIndexed { index, ends ->
            compose.onNodeWithTag(END_COUNT_SLIDER_TAG)
                .performSemanticsAction(SemanticsActions.SetProgress) { it(index.toFloat()) }
            compose.onNodeWithText(label(ends)).assertIsDisplayed()
        }
    }
}
