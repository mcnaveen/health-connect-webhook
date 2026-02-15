package com.hcwebhook.app.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.hcwebhook.app.HealthConnectManager
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.SyncManager
import com.hcwebhook.app.SyncResult
import kotlinx.coroutines.launch

@Composable
fun ManualSyncCard(onSyncCompleted: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    
    // We need to know if webhooks are configured to enable the button
    // This might not be reactive if configs change elsewhere, but for now it's okay 
    // or we can pass it in/observe it. 
    // Given the simplicity, let's fetch it.
    val webhookConfigs = preferencesManager.getWebhookConfigs()
    
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Manual Sync", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Trigger a manual sync to send current health data to webhooks",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isSyncing) return@Button

                    scope.launch {
                        isSyncing = true
                        syncMessage = null

                        try {
                            // Check Health Connect SDK status
                            val availability = HealthConnectClient.getSdkStatus(context)
                            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                                syncMessage = when (availability) {
                                    HealthConnectClient.SDK_UNAVAILABLE -> "Health Connect is not installed"
                                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect needs update"
                                    else -> "Health Connect is not available"
                                }
                                isSyncing = false
                                return@launch
                            }

                            val healthConnectManager = HealthConnectManager(context)
                            if (!healthConnectManager.hasPermissions()) {
                                syncMessage = "Permissions required for sync."
                                isSyncing = false
                                return@launch
                            }
                            
                            val syncManager = SyncManager(context)
                            val result = syncManager.performSync()

                            when {
                                result.isSuccess -> {
                                    val syncResult = result.getOrThrow()
                                    syncMessage = when (syncResult) {
                                        is SyncResult.NoData -> "No new data to sync"
                                        is SyncResult.Success -> {
                                            val parts = syncResult.syncCounts.map { (type, count) ->
                                                "$count ${type.displayName.lowercase()}"
                                            }
                                            if (parts.isEmpty()) {
                                                "Sync completed successfully"
                                            } else {
                                                "Synced ${parts.joinToString(", ")}"
                                            }
                                        }
                                    }
                                    onSyncCompleted()
                                }
                                result.isFailure -> {
                                    syncMessage = "Sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                }
                            }
                        } catch (e: Exception) {
                            syncMessage = "Sync failed: ${e.message}"
                        } finally {
                            isSyncing = false
                        }
                    }
                },
                enabled = !isSyncing && webhookConfigs.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing...")
                } else {
                    Text("Sync Now")
                }
            }

            syncMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.contains("failed", ignoreCase = true))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
