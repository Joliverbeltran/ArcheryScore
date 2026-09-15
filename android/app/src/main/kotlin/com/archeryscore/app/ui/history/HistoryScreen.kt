package com.archeryscore.app.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscore.app.R
import com.archeryscore.app.domain.model.Session
import com.archeryscore.app.domain.model.SessionStatus
import java.time.Instant

@Composable
fun HistoryScreen(
    onOpenDetail: (String) -> Unit,
    padding: PaddingValues,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val importText = when (val message = importMessage) {
        null -> null
        is ImportMessage.Success -> stringResource(R.string.import_result, message.imported, message.skipped)
        is ImportMessage.Failure ->
            stringResource(R.string.import_result_error, message.row, message.column, message.reason)
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return@rememberLauncherForActivityResult
        viewModel.importCsv(content)
    }

    LaunchedEffect(importText) {
        val text = importText
        if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.clearImportMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("text/csv")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.import_csv)) }
        Spacer(Modifier.height(12.dp))
        when {
            state.loading -> CircularProgressIndicator()
            state.sessions.isEmpty() -> Text(stringResource(R.string.empty_history))
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.sessions, key = { it.id.toString() }) { session ->
                    HistoryRow(session) { onOpenDetail(session.id.toString()) }
                }
            }
        }
    }
    SnackbarHost(hostState = snackbar, modifier = Modifier.padding(padding))
}

@Composable
private fun HistoryRow(session: Session, onClick: () -> Unit) {
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
                Text(formatDate(session.date), style = MaterialTheme.typography.titleSmall)
                Text(
                    "${session.discipline.name} · ${session.roundType.name} · ${session.distanceM} m",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            stringResource(
                                if (session.status == SessionStatus.ACTIVE) R.string.status_active else R.string.status_complete
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