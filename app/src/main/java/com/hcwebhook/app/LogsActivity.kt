package com.hcwebhook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.ui.theme.HCWebhookTheme
import java.text.SimpleDateFormat
import java.util.*

class LogsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HCWebhookTheme {
                LogsScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LogsScreen() {
        val context = LocalContext.current
        val preferencesManager = remember { PreferencesManager(context) }
        var logs by remember { mutableStateOf(preferencesManager.getWebhookLogs()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Webhook Logs") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (logs.isNotEmpty()) {
                            TextButton(onClick = {
                                preferencesManager.clearWebhookLogs()
                                logs = emptyList()
                            }) {
                                Text("Clear")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "No webhook logs yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(logs) { log ->
                        LogItem(log)
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun LogItem(log: WebhookLog) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (log.success) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Timestamp
                Text(
                    formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (log.success) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // URL
                Text(
                    log.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (log.success) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (log.success) {
                            "Success • ${log.statusCode ?: "?"}"
                        } else {
                            "Failed • ${log.statusCode ?: "Error"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (log.success) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    if (log.dataType != null && log.recordCount != null) {
                        Text(
                            "${log.dataType}: ${log.recordCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (log.success) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }

                // Error message
                if (!log.success && log.errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        log.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
