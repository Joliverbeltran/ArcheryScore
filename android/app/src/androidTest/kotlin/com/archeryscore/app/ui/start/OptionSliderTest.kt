package com.archeryscore.app.ui.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.SessionSetupOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OptionSliderTest {

    @get:Rule
    val compose = createComposeRule()

    private val tag = "option_slider_test"
    private val values = SessionSetupOptions.DISTANCES_M

    private fun hasProgressRange(range: ClosedFloatingPointRange<Float>) =
        SemanticsMatcher("has progress range $range") { node ->
            val info = node.config.getOrElseNullable(SemanticsProperties.ProgressBarRangeInfo) { null }
            info != null && info.range == range
        }

    @Test
    fun exposesASliderWithProgressSemantics() {
        compose.setContent {
            OptionSlider(
                labelRes = R.string.distance_m,
                values = values,
                selectedIndex = 0,
                onIndexChange = {},
                valueText = "8 m",
                testTag = tag,
            )
        }

        compose.onNodeWithTag(tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        compose.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    }

    @Test
    fun optionIndicesSpanAnEvenlySpacedRange() {
        compose.setContent {
            OptionSlider(
                labelRes = R.string.distance_m,
                values = values,
                selectedIndex = 0,
                onIndexChange = {},
                valueText = "8 m",
                testTag = tag,
            )
        }

        compose.onNodeWithTag(tag).assert(hasProgressRange(0f..values.lastIndex.toFloat()))
    }

    @Test
    fun displaysTheProvidedValueText() {
        compose.setContent {
            OptionSlider(
                labelRes = R.string.distance_m,
                values = values,
                selectedIndex = 3,
                onIndexChange = {},
                valueText = "30 m",
                testTag = tag,
            )
        }

        compose.onNodeWithText("30 m").assertIsDisplayed()
    }

    @Test
    fun semanticsNameTheControlAndDescribeTheCurrentValue() {
        compose.setContent {
            OptionSlider(
                labelRes = R.string.distance_m,
                values = values,
                selectedIndex = 3,
                onIndexChange = {},
                valueText = "30 m",
                testTag = tag,
            )
        }

        compose.onNodeWithTag(tag).assertContentDescriptionEquals("Distance (m)")
        compose.onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "30 m"))
    }

    @Test
    fun settingProgressReportsASnappedOptionIndex() {
        var reported = -1
        compose.setContent {
            var index by remember { mutableIntStateOf(0) }
            OptionSlider(
                labelRes = R.string.distance_m,
                values = values,
                selectedIndex = index,
                onIndexChange = { reported = it; index = it },
                valueText = "8 m",
                testTag = tag,
            )
        }

        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(3.4f) }
        compose.runOnIdle { assertEquals(3, reported) }
    }

    @Test
    fun selectingAnOptionUpdatesTheValueText() {
        compose.setContent {
            var index by remember { mutableIntStateOf(0) }
            val value = SessionSetupOptions.distanceAtIndex(index)
            OptionSlider(
                labelRes = R.string.distance_m,
                values = values,
                selectedIndex = index,
                onIndexChange = { index = it },
                valueText = value.toString(),
                testTag = tag,
            )
        }

        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
        compose.runOnIdle {
            assertEquals(90, SessionSetupOptions.distanceAtIndex(7))
        }
        compose.onNodeWithText("90").assertIsDisplayed()
    }
}
