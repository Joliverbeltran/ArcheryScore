package com.archeryscore.app.ui.history

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.archeryscore.app.R
import com.archeryscore.app.domain.repository.EndWithArrows
import com.archeryscore.app.domain.repository.SessionDetail
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    padding: PaddingValues,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(8.dp))
        val detail = state.detail
        if (detail == null) {
            CircularProgressIndicator()
        } else {
            DetailHeader(detail)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(detail.ends, key = { it.end.id.toString() }) { end -> EndRow(end) }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        val success = runCatching {
                            shareCsv(context, viewModel.exportCsv(detail), viewModel.sessionId)
                        }.isSuccess
                        if (!success) snackbar.showSnackbar(context.getString(R.string.export_error))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.export_csv)) }
        }
    }
    SnackbarHost(hostState = snackbar, modifier = Modifier.padding(padding))
}

@Composable
private fun DetailHeader(detail: SessionDetail) {
    val s = detail.session
    Column {
        Text("${s.discipline.name} · ${s.roundType.name} · ${s.distanceM} m", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(stringResource(R.string.running_total) + ": ${detail.total}", style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.x_count) + ": ${detail.xCount}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EndRow(end: EndWithArrows) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.arrow_format, end.end.endNumber), style = MaterialTheme.typography.titleSmall)
            Text(
                end.arrows.joinToString("  ") { if (it.isXRing) "${it.score}X" else "${it.score}" },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun shareCsv(context: android.content.Context, csv: String, sessionId: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "session-$sessionId.csv")
    file.writeText(csv, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
}