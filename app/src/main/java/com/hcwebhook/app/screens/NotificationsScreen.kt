package com.hcwebhook.app.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    var notificationConfigs by remember { mutableStateOf(prefsManager.getNotificationConfigs()) }

    var showProviderPicker by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<NotificationConfig?>(null) }
    var testLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(notificationConfigs) {
        prefsManager.setNotificationConfigs(notificationConfigs)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showProviderPicker = true }) {
                Icon(Icons.Filled.Add, "Add Notification")
            }
        }
    ) { padding ->
        if (notificationConfigs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notification providers configured.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(notificationConfigs) { config ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = config.providerType.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = config.displayIdentifier,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    Switch(
                                        checked = config.isEnabled,
                                        onCheckedChange = { isEnabled ->
                                            notificationConfigs = notificationConfigs.map {
                                                if (it.id == config.id) it.copy(isEnabled = isEnabled) else it
                                            }
                                        }
                                    )
                                    IconButton(onClick = { editingConfig = config }) {
                                        Icon(Icons.Filled.Edit, "Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Provider Picker ───────────────────────────────────────────────────
        if (showProviderPicker) {
            ModalBottomSheet(onDismissRequest = { showProviderPicker = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Select Provider",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn {
                        items(NotificationProviderType.values()) { provider ->
                            ListItem(
                                headlineContent = { Text(provider.displayName) },
                                supportingContent = { Text(provider.description) },
                                modifier = Modifier.clickable {
                                    showProviderPicker = false
                                    editingConfig = NotificationConfig(providerType = provider)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Editor ────────────────────────────────────────────────────────────
        if (editingConfig != null) {
            val config = editingConfig!!
            var editUrl by remember { mutableStateOf(config.url) }
            var editToken by remember { mutableStateOf(config.token) }
            var editTopic by remember { mutableStateOf(config.topic) }
            var editChatId by remember { mutableStateOf(config.chatId) }
            var editUserKey by remember { mutableStateOf(config.userKey) }
            var editBodyTemplate by remember { mutableStateOf(config.bodyTemplate) }
            var editHeaders by remember { mutableStateOf(config.headers) }
            var newHeaderKey by remember { mutableStateOf("") }
            var newHeaderValue by remember { mutableStateOf("") }

            ModalBottomSheet(onDismissRequest = { editingConfig = null }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "${config.providerType.displayName} Configuration",
                        style = MaterialTheme.typography.titleMedium
                    )

                    when (config.providerType) {
                        NotificationProviderType.GOTIFY -> {
                            OutlinedTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                label = { Text("Server URL (e.g., https://gotify.example.com)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editToken,
                                onValueChange = { editToken = it },
                                label = { Text("App Token") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        NotificationProviderType.NTFY -> {
                            OutlinedTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                label = { Text("Server URL (e.g., https://ntfy.sh)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editTopic,
                                onValueChange = { editTopic = it },
                                label = { Text("Topic Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        NotificationProviderType.TELEGRAM -> {
                            OutlinedTextField(
                                value = editToken,
                                onValueChange = { editToken = it },
                                label = { Text("Bot Token") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editChatId,
                                onValueChange = { editChatId = it },
                                label = { Text("Chat ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        NotificationProviderType.DISCORD -> {
                            OutlinedTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                label = { Text("Webhook URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        NotificationProviderType.PUSHOVER -> {
                            OutlinedTextField(
                                value = editToken,
                                onValueChange = { editToken = it },
                                label = { Text("App Token") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editUserKey,
                                onValueChange = { editUserKey = it },
                                label = { Text("User Key") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        NotificationProviderType.CUSTOM_HTTP -> {
                            OutlinedTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                label = { Text("Endpoint URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editBodyTemplate,
                                onValueChange = { editBodyTemplate = it },
                                label = { Text("Body Template (JSON)") },
                                placeholder = { Text("{\"title\":\"{title}\",\"message\":\"{message}\"}") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("{title}", "{message}", "{status}").forEach { v ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { editBodyTemplate += v },
                                        label = { Text(v, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            HorizontalDivider()
                            Text("Headers", style = MaterialTheme.typography.labelMedium)
                            if (editHeaders.isEmpty()) {
                                Text(
                                    "No headers added",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                editHeaders.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Text(v, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(
                                            onClick = { editHeaders = editHeaders - k },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, null, Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newHeaderKey,
                                    onValueChange = { newHeaderKey = it },
                                    label = { Text("Key") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = newHeaderValue,
                                    onValueChange = { newHeaderValue = it },
                                    label = { Text("Value") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()) {
                                        editHeaders = editHeaders + (newHeaderKey.trim() to newHeaderValue.trim())
                                        newHeaderKey = ""
                                        newHeaderValue = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Header")
                            }
                        }
                        NotificationProviderType.LOCAL_PUSH -> {
                            Text(
                                "Posts an Android system notification after each sync. Tap it to open logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                testLoading = true
                                val testConfig = config.copy(
                                    url = editUrl,
                                    token = editToken,
                                    topic = editTopic,
                                    chatId = editChatId,
                                    userKey = editUserKey,
                                    bodyTemplate = editBodyTemplate,
                                    headers = editHeaders,
                                    isEnabled = true
                                )
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        NotificationDispatcher().dispatch(
                                            context,
                                            testConfig,
                                            "Test Notification",
                                            "This is a test message from HC Webhook."
                                        )
                                    }
                                    testLoading = false
                                    Toast.makeText(context, "Test notification dispatched", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !testLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (testLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Test")
                        }

                        Button(
                            onClick = {
                                val newConfig = config.copy(
                                    url = editUrl,
                                    token = editToken,
                                    topic = editTopic,
                                    chatId = editChatId,
                                    userKey = editUserKey,
                                    bodyTemplate = editBodyTemplate,
                                    headers = editHeaders
                                )
                                val exists = notificationConfigs.any { it.id == newConfig.id }
                                notificationConfigs = if (exists) {
                                    notificationConfigs.map { if (it.id == newConfig.id) newConfig else it }
                                } else {
                                    notificationConfigs + newConfig
                                }
                                editingConfig = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }

                    if (notificationConfigs.any { it.id == config.id }) {
                        TextButton(
                            onClick = {
                                notificationConfigs = notificationConfigs.filter { it.id != config.id }
                                editingConfig = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Configuration")
                        }
                    }
                }
            }
        }
    }
}
