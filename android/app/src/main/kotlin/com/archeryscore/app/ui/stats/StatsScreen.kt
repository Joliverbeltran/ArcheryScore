package com.archeryscore.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscore.app.R
import com.archeryscore.app.domain.usecase.DisciplineStats

@Composable
fun StatsScreen(
    padding: PaddingValues,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.aggregate.totalSessions == 0) {
            Text(stringResource(R.string.empty_stats))
        } else {
            val a = state.aggregate
            StatCard(stringResource(R.string.total_sessions), "${a.totalSessions}")
            StatCard(stringResource(R.string.total_arrows), "${a.totalArrows}")
            StatCard(stringResource(R.string.total_score), "${a.totalScore}")
            StatCard(stringResource(R.string.best_session_score), "${a.bestSessionScore ?: 0}")
            StatCard(stringResource(R.string.average_score), "${a.averageScore}")
            StatCard(stringResource(R.string.average_accuracy), "${a.averageAccuracy}")

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.per_discipline), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            state.byDiscipline.forEach { d ->
                StatCard(d.discipline.name, "${d.averageScore} (${d.sessionCount})")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}