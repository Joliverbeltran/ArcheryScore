package com.archeryscore.app.ui.start

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun OptionSlider(
    @StringRes labelRes: Int,
    values: List<Int>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    valueText: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    require(values.size >= 2) { "OptionSlider needs at least two options" }
    val label = stringResource(labelRes)
    val lastIndex = values.lastIndex
    val index = selectedIndex.coerceIn(0, lastIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(text = valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = index.toFloat(),
            onValueChange = { onIndexChange(it.roundToInt().coerceIn(0, lastIndex)) },
            valueRange = 0f..lastIndex.toFloat(),
            steps = lastIndex - 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                }
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
    }
}
