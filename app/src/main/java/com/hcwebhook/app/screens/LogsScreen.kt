package com.hcwebhook.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.WebhookLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    // We should probably make logs observable or reload on resume, but for now simple state is fine
    var logs by remember { mutableStateOf(preferencesManager.getWebhookLogs()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "Webhook Logs",
                style = MaterialTheme.typography.titleLarge
            )
            if (logs.isNotEmpty()) {
                TextButton(onClick = {
                    preferencesManager.clearWebhookLogs()
                    logs = emptyList()
                }) {
                    Text("Clear")
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No logs yet",
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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                text = "${log.statusCode ?: "ERR"}",
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
