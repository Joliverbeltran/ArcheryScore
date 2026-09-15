package com.archeryscore.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.Arrow
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail

@Composable
fun ActiveSessionScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    padding: PaddingValues,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsState()
    var editing by remember { mutableStateOf<Arrow?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }

    val content = detail
    if (content == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.no_active_session))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
            }
            Text(content.session.disciplineLabel(), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        SessionHeader(content)
        Spacer(Modifier.height(12.dp))
        TotalRow(content)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(content.ends, key = { it.end.id.toString() }) { end ->
                EndCard(end, content.session.roundType) { editing = it }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::addEnd,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.add_end)) }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { confirmFinish = true },
            enabled = viewModel.isFullyScored(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.finish_session)) }
    }

    editing?.let { arrow ->
        ScoreDialog(
            arrow = arrow,
            roundType = content.session.roundType,
            onDismiss = { editing = null },
            onSave = { score, isX ->
                viewModel.recordArrow(arrow, score, isX)
                editing = null
            },
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.finish_dialog_title)) },
            text = { Text(stringResource(R.string.finish_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    viewModel.confirmCompletion()
                    onFinished()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SessionHeader(detail: SessionDetail) {
    val s = detail.session
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                "${s.roundType.label()} · ${s.distanceM} m",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(s.disciplineLabel(), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            stringResource(R.string.end_format, s.endCount, s.endCount),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TotalRow(detail: SessionDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(stringResource(R.string.running_total), style = MaterialTheme.typography.labelMedium)
            Text("${detail.total}", style = MaterialTheme.typography.headlineSmall)
        }
        Column {
            Text(stringResource(R.string.x_count), style = MaterialTheme.typography.labelMedium)
            Text("${detail.xCount}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun EndCard(
    end: EndWithArrows,
    roundType: RoundType,
    onEdit: (Arrow) -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.arrow_format, end.end.endNumber), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                end.arrows.forEach { arrow ->
                    FilterChip(
                        selected = false,
                        onClick = { onEdit(arrow) },
                        label = { Text(if (arrow.isXRing) "${arrow.score}X" else "${arrow.score}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreDialog(
    arrow: Arrow,
    roundType: RoundType,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean) -> Unit,
) {
    var score by remember { mutableIntStateOf(arrow.score) }
    var isX by remember { mutableStateOf(arrow.isXRing) }
    val max = when (roundType) {
        RoundType.TEN_ZONE -> 10
        RoundType.FIVE_ZONE -> 5
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.score_arrow, arrow.arrowNumber)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..max).forEach { value ->
                        FilterChip(
                            selected = score == value,
                            onClick = {
                                score = value
                                if (value != max) isX = false
                            },
                            label = { Text("$value") },
                        )
                    }
                }
                if (roundType == RoundType.TEN_ZONE) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.x_ring))
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = isX, onCheckedChange = { isX = it && score == max })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(score, isX) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun com.archeryscore.app.domain.model.Session.disciplineLabel(): String =
    stringResource(
        when (discipline) {
            com.archeryscore.app.domain.model.Discipline.OLYMPIC_RECURVE -> R.string.olympic_recurve
            com.archeryscore.app.domain.model.Discipline.TRADITIONAL_RECURVE -> R.string.traditional_recurve
            com.archeryscore.app.domain.model.Discipline.BAREBOW -> R.string.barebow
            com.archeryscore.app.domain.model.Discipline.LONGBOW -> R.string.longbow
            com.archeryscore.app.domain.model.Discipline.COMPOUND -> R.string.compound
        }
    )

private fun com.archeryscore.app.domain.model.RoundType.label(): String = when (this) {
    RoundType.TEN_ZONE -> "0-10"
    RoundType.FIVE_ZONE -> "0-5"
}