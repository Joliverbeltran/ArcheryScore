package com.archeryscore.app.ui.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.archeryscore.app.domain.model.TargetType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartScreenTargetTypeTest {

    @get:Rule
    val compose = createComposeRule()

    private val labels = mapOf(
        TargetType.CM122 to "122 cm",
        TargetType.CM80 to "80 cm",
        TargetType.CM60 to "60 cm",
        TargetType.CM40 to "40 cm",
        TargetType.TRIPLE_VERTICAL to "Triple vertical",
        TargetType.TRIPLE_TRIANGULAR to "Triple triangular",
    )

    @Test
    fun dropdownListsExactlySixTargetOptions() {
        compose.setContent {
            var selected by remember { mutableStateOf(TargetType.CM122) }
            TargetTypeDropdown(selected = selected, onSelect = { selected = it })
        }
        compose.onNodeWithTag(TARGET_TYPE_TAG).performClick()
        compose.waitForIdle()

        labels.forEach { (targetType, label) ->
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        assertEquals(6, labels.size)
    }

    @Test
    fun preSelectedFromDefaultTargetType() {
        compose.setContent {
            var selected by remember { mutableStateOf(TargetType.CM80) }
            TargetTypeDropdown(selected = selected, onSelect = { selected = it })
        }
        val field = compose.onNodeWithTag(TARGET_TYPE_TAG)
        field.assertTextEquals("80 cm")
    }

    @Test
    fun selectingAnOptionUpdatesTheSelection() {
        compose.setContent {
            var selected by remember { mutableStateOf(TargetType.CM122) }
            TargetTypeDropdown(selected = selected, onSelect = { selected = it })
        }
        compose.onNodeWithTag(TARGET_TYPE_TAG).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Triple vertical").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TARGET_TYPE_TAG).assertTextEquals("Triple vertical")
    }

    companion object {
        const val TARGET_TYPE_TAG = "target_type_dropdown"
    }
}