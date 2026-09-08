package com.archeryscore.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
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
import com.archeryscore.app.domain.model.SessionStatus
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.repository.SessionListItem
import java.time.Instant

@Composable
fun HistoryScreen(
    onOpenDetail: (String) -> Unit,
    padding: PaddingValues,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        when {
            state.loading -> CircularProgressIndicator()
            state.sessions.isEmpty() -> Text(stringResource(R.string.empty_history))
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.sessions, key = { it.session.id.toString() }) { item ->
                    HistoryRow(item) { onOpenDetail(item.session.id.toString()) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: SessionListItem, onClick: () -> Unit) {
    val s = item.session
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(formatDate(s.date), style = MaterialTheme.typography.titleSmall)
                Text(
                    "${s.discipline.name} · ${s.roundType.name} · ${s.distanceM} m",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            stringResource(
                                if (s.status == SessionStatus.ACTIVE) R.string.status_active else R.string.status_complete
                            )
                        )
                    },
                )
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            stringResource(
                                if (item.syncStatus == SyncStatus.SYNCED) R.string.sync_synced else R.string.sync_pending
                            )
                        )
                    },
                )
            }
        }
    }
}

private fun formatDate(instant: Instant): String =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
        .format(instant)