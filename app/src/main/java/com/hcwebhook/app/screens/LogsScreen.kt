package com.hcwebhook.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import com.hcwebhook.app.WebhookLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var logs by remember { mutableStateOf(preferencesManager.getWebhookLogs()) }
    var showClearSheet by remember { mutableStateOf(false) }

    // ── Clear Logs Confirmation Bottom Sheet ──────────────────────────────────
    if (showClearSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClearSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Text(stringResource(R.string.logs_clear_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.logs_clear_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        preferencesManager.clearWebhookLogs()
                        logs = emptyList()
                        showClearSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_clear_logs))
                }
                OutlinedButton(
                    onClick = { showClearSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.logs_title),
                style = MaterialTheme.typography.titleLarge
            )
            if (logs.isNotEmpty()) {
                TextButton(onClick = { showClearSheet = true }) {
                    Text(stringResource(R.string.action_clear))
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.logs_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(logs) { log ->
                    LogItem(log)
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for nav bar
                }
            }
        }
    }
}

@Composable
private fun LogItem(log: WebhookLog) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = log.url,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTimestamp(log.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (log.dataType != null && log.recordCount != null) {
                         Text(
                            " • ${log.recordCount} ${log.dataType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Status
            val statusColor = if (log.success) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) 
            else 
                MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            
            Text(
                text = "${log.statusCode ?: stringResource(R.string.logs_status_err)}",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }

        if (!log.success && log.errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                log.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) // Shorter timestamp
    return sdf.format(Date(timestamp))
}
