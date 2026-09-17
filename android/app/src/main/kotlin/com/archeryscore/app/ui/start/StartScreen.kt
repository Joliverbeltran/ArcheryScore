package com.archeryscore.app.ui.start

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.TargetType
import com.archeryscore.app.ui.resume.ResumeSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onResumeActive: (String) -> Unit,
    onOpenHistory: () -> Unit,
    padding: PaddingValues,
    viewModel: ResumeSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.activeSessionId) {
        val id = state.activeSessionId
        if (id != null && !state.loading) {
            onResumeActive(id)
        }
    }

    var discipline by remember { mutableStateOf(state.defaults.defaultDiscipline) }
    var roundType by remember { mutableStateOf(state.defaults.defaultRoundType) }
    var targetType by remember { mutableStateOf(state.defaults.defaultTargetType) }
    var distance by remember { mutableIntStateOf(state.defaults.defaultDistanceM) }
    var endCount by remember { mutableIntStateOf(state.defaults.defaultEndCount) }
    var arrowsPerEnd by remember { mutableIntStateOf(state.defaults.defaultArrowsPerEnd) }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        when {
            state.loading -> {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }

            state.activeSessionId != null -> {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { state.activeSessionId?.let(onResumeActive) }) {
                    Text(stringResource(R.string.resume_active))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.new_session), style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
        DisciplineDropdown(discipline) { discipline = it }

        Spacer(Modifier.height(8.dp))
        RoundTypeDropdown(roundType) { roundType = it }

        Spacer(Modifier.height(8.dp))
        TargetTypeDropdown(targetType) { targetType = it }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = distance.toString(),
            onValueChange = { distance = it.toIntOrNull() ?: 18 },
            label = { Text(stringResource(R.string.distance_m)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = endCount.toString(),
            onValueChange = { endCount = (it.toIntOrNull() ?: 6).coerceIn(1, 60) },
            label = { Text(stringResource(R.string.end_count)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = arrowsPerEnd.toString(),
            onValueChange = { arrowsPerEnd = (it.toIntOrNull() ?: 3).coerceIn(1, 12) },
            label = { Text(stringResource(R.string.arrows_per_end)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.createSession(
                    roundType = roundType,
                    targetType = targetType,
                    endCount = endCount,
                    arrowsPerEnd = arrowsPerEnd,
                    distanceM = distance,
                    discipline = discipline,
                    notes = notes.ifBlank { null },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.start_session))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetTypeDropdown(
    selected: TargetType,
    onSelect: (TargetType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.target_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag(TARGET_TYPE_DROPDOWN_TAG),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TargetType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(stringResource(t.labelRes)) },
                    onClick = { onSelect(t); expanded = false },
                )
            }
        }
    }
}

private fun disciplineLabel(discipline: Discipline): Int = when (discipline) {
    Discipline.OLYMPIC_RECURVE -> R.string.olympic_recurve
    Discipline.TRADITIONAL_RECURVE -> R.string.traditional_recurve
    Discipline.BAREBOW -> R.string.barebow
    Discipline.LONGBOW -> R.string.longbow
    Discipline.COMPOUND -> R.string.compound
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisciplineDropdown(
    selected: Discipline,
    onSelect: (Discipline) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(disciplineLabel(selected)),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.discipline)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Discipline.entries.forEach { d ->
                DropdownMenuItem(
                    text = { Text(stringResource(disciplineLabel(d))) },
                    onClick = { onSelect(d); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundTypeDropdown(
    selected: RoundType,
    onSelect: (RoundType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (selected == RoundType.TEN_ZONE) stringResource(R.string.ten_zone) else stringResource(R.string.five_zone),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.round_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ten_zone)) },
                onClick = { onSelect(RoundType.TEN_ZONE); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.five_zone)) },
                onClick = { onSelect(RoundType.FIVE_ZONE); expanded = false },
)
        }
    }
}

const val TARGET_TYPE_DROPDOWN_TAG = "target_type_dropdown"