package com.hcwebhook.app.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.hcwebhook.app.R
import com.hcwebhook.app.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhooksScreen(
    activity: MainActivity
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    // State
    var webhookConfigs by remember { mutableStateOf(preferencesManager.getWebhookConfigs()) }
    var newUrl by remember { mutableStateOf("") }
    
    // Dialog State
    var showHeaderDialog by remember { mutableStateOf(false) }
    var selectedConfigIndex by remember { mutableStateOf(-1) }

    // Delete confirmation bottom sheet state
    var showDeleteSheet by remember { mutableStateOf(false) }
    var pendingDeleteIndex by remember { mutableStateOf(-1) }

    // Save changes when webhookConfigs changes
    LaunchedEffect(webhookConfigs) {
        preferencesManager.setWebhookConfigs(webhookConfigs)
    }

    val scrollState = rememberScrollState()

    // ── Delete Confirmation Bottom Sheet ──────────────────────────────────────
    if (showDeleteSheet && pendingDeleteIndex in webhookConfigs.indices) {
        val urlToDelete = webhookConfigs[pendingDeleteIndex].url
        ModalBottomSheet(
            onDismissRequest = {
                showDeleteSheet = false
                pendingDeleteIndex = -1
            }
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
                Text(stringResource(R.string.webhooks_delete_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.webhooks_delete_desc, urlToDelete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        webhookConfigs = webhookConfigs.toMutableList().apply { removeAt(pendingDeleteIndex) }
                        showDeleteSheet = false
                        pendingDeleteIndex = -1
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_delete))
                }
                OutlinedButton(
                    onClick = {
                        showDeleteSheet = false
                        pendingDeleteIndex = -1
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Webhook URLs Section
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.webhooks_section_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (webhookConfigs.isEmpty()) {
                        Text(
                            stringResource(R.string.webhooks_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Current URLs
                    webhookConfigs.forEachIndexed { index, config ->
                        Column {
                             Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = config.url,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val headerCount = config.getHeaderCount()
                                    if (headerCount > 0) {
                                        Text(
                                            text = stringResource(R.string.webhooks_headers_count, headerCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        selectedConfigIndex = index
                                        showHeaderDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, stringResource(R.string.webhooks_action_edit_headers))
                                    }
                                    IconButton(onClick = {
                                        pendingDeleteIndex = index
                                        showDeleteSheet = true
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Add new URL
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text(stringResource(R.string.webhooks_new_url_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newUrl.isNotBlank() && (newUrl.startsWith("http://") || newUrl.startsWith("https://"))) {
                                webhookConfigs = webhookConfigs + WebhookConfig.fromUrl(newUrl)
                                newUrl = ""
                            } else {
                                Toast.makeText(context, context.getString(R.string.webhooks_toast_invalid_url), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.webhooks_action_add))
                    }
                }
            }
        }
    }
    
    // Header Dialog
    if (showHeaderDialog && selectedConfigIndex in webhookConfigs.indices) {
        val config = webhookConfigs[selectedConfigIndex]
        var currentHeaders by remember { mutableStateOf(config.headers) }
        var newKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showHeaderDialog = false },
            title = { Text(stringResource(R.string.webhooks_headers_manage_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.webhooks_headers_for_url, config.url), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Existing Headers
                    if (currentHeaders.isEmpty()) {
                        Text(stringResource(R.string.webhooks_headers_empty), style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                    
                    currentHeaders.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(value, style = MaterialTheme.typography.bodySmall) 
                            }
                            IconButton(onClick = {
                                currentHeaders = currentHeaders - key
                            }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.webhooks_headers_action_remove), modifier = Modifier.size(20.dp))
                            }
                        }
                        Divider()
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.webhooks_headers_action_add_title), style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text(stringResource(R.string.webhooks_headers_key_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text(stringResource(R.string.webhooks_headers_value_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                currentHeaders = currentHeaders + (newKey.trim() to newValue.trim())
                                newKey = ""
                                newValue = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.webhooks_headers_action_add_title))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedConfig = config.copy(headers = currentHeaders)
                    val newList = webhookConfigs.toMutableList()
                    newList[selectedConfigIndex] = updatedConfig
                    webhookConfigs = newList
                    showHeaderDialog = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHeaderDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
