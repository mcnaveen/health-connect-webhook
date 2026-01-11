package com.hcwebhook.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shield
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hcwebhook.app.ui.theme.HCWebhookTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private var pendingSyncCallback: (() -> Unit)? = null
    private var permissionStatusCallback: ((Boolean) -> Unit)? = null
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>

    private fun initializePermissionLauncher() {
        val requestPermissionActivityContract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

        permissionLauncher = registerForActivityResult(requestPermissionActivityContract) { granted: Set<String> ->
            lifecycleScope.launch {
                val healthConnectManager = HealthConnectManager(this@MainActivity)
                val grantedPermissions = healthConnectManager.getGrantedPermissions()
                val hasAnyPerms = grantedPermissions.isNotEmpty()

                permissionStatusCallback?.invoke(hasAnyPerms)

                if (hasAnyPerms && pendingSyncCallback != null) {
                    pendingSyncCallback?.invoke()
                    pendingSyncCallback = null
                } else if (!hasAnyPerms && pendingSyncCallback != null) {
                    Toast.makeText(this@MainActivity, "No permissions granted", Toast.LENGTH_LONG).show()
                    pendingSyncCallback = null
                }
            }
        }
    }

    companion object { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(this)
        initializePermissionLauncher()

        setContent {
            HCWebhookTheme {
                ConfigurationScreen(
                    activity = this@MainActivity,
                    permissionLauncher = permissionLauncher
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConfigurationScreen(
        activity: MainActivity,
        permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Store initial values to detect changes (mutable so we can update after saving)
        var initialSyncInterval by remember { mutableStateOf(preferencesManager.getSyncIntervalMinutes()) }
        var initialWebhookUrls by remember { mutableStateOf(preferencesManager.getWebhookUrls()) }
        var initialEnabledDataTypes by remember { mutableStateOf(preferencesManager.getEnabledDataTypes()) }

        var syncInterval by remember { mutableStateOf(initialSyncInterval.toString()) }
        var webhookUrls by remember { mutableStateOf(initialWebhookUrls) }
        var newUrl by remember { mutableStateOf("") }
        var isSyncing by remember { mutableStateOf(false) }
        var syncMessage by remember { mutableStateOf<String?>(null) }
        var hasPermissions by remember { mutableStateOf<Boolean?>(null) }
        var enabledDataTypes by remember { mutableStateOf(initialEnabledDataTypes) }
        var grantedPermissionsSet by remember { mutableStateOf<Set<String>>(emptySet()) }
        var showPermissionModal by remember { mutableStateOf(false) }
        var selectedDataTypeForPermission by remember { mutableStateOf<HealthDataType?>(null) }
        var isDataTypesExpanded by remember { mutableStateOf(false) }

        // Check if configuration has changed
        val hasChanges = remember(syncInterval, webhookUrls, enabledDataTypes, initialSyncInterval, initialWebhookUrls, initialEnabledDataTypes) {
            val currentInterval = syncInterval.toIntOrNull() ?: initialSyncInterval
            val intervalChanged = currentInterval != initialSyncInterval
            val urlsChanged = webhookUrls != initialWebhookUrls
            val dataTypesChanged = enabledDataTypes != initialEnabledDataTypes
            intervalChanged || urlsChanged || dataTypesChanged
        }

        // Show toast when changes are detected
        var previousHasChanges by remember { mutableStateOf(false) }
        LaunchedEffect(hasChanges) {
            if (hasChanges && !previousHasChanges) {
                Toast.makeText(context, "Save the changes", Toast.LENGTH_SHORT).show()
            }
            previousHasChanges = hasChanges
        }

        val scrollState = rememberScrollState()

        // Check permissions on startup and set callback
        LaunchedEffect(Unit) {
            activity.permissionStatusCallback = { granted ->
                hasPermissions = granted
                // Refresh granted permissions set when permissions change
                if (granted) {
                    scope.launch {
                        try {
                            val healthConnectManager = HealthConnectManager(context)
                            grantedPermissionsSet = healthConnectManager.getGrantedPermissions()
                        } catch (e: Exception) {
                            // Silent fail
                        }
                    }
                } else {
                    grantedPermissionsSet = emptySet()
                }
            }

            try {
                val availability = HealthConnectClient.getSdkStatus(context)
                if (availability != HealthConnectClient.SDK_AVAILABLE) {
                    hasPermissions = false
                    return@LaunchedEffect
                }

                val healthConnectManager = HealthConnectManager(context)
                val grantedPermissions = healthConnectManager.getGrantedPermissions()
                hasPermissions = grantedPermissions.isNotEmpty()
                grantedPermissionsSet = grantedPermissions

                // Auto-enable switches for granted permissions if none enabled yet
                if (enabledDataTypes.isEmpty() && grantedPermissions.isNotEmpty()) {
                    val grantedTypes = HealthDataType.entries.filter { type ->
                        HealthPermission.getReadPermission(type.recordClass) in grantedPermissions
                    }.toSet()
                    if (grantedTypes.isNotEmpty()) {
                        enabledDataTypes = grantedTypes
                        preferencesManager.setEnabledDataTypes(grantedTypes)
                    }
                }
            } catch (e: Exception) {
                hasPermissions = false
            }
        }

        LaunchedEffect(hasPermissions) {
            if (hasPermissions == true) {
                try {
                    val healthConnectManager = HealthConnectManager(context)
                    grantedPermissionsSet = healthConnectManager.getGrantedPermissions()
                } catch (e: Exception) {
                    // Silent fail
                }
            }
        }

        // Calculate missing permissions for enabled data types
        val missingPermissionsForEnabled = remember(enabledDataTypes, grantedPermissionsSet) {
            enabledDataTypes.mapNotNull { dataType ->
                val permission = HealthPermission.getReadPermission(dataType.recordClass)
                if (permission !in grantedPermissionsSet) permission else null
            }.toSet()
        }

        // Check if at least one permission is granted
        val hasAtLeastOnePermission = grantedPermissionsSet.isNotEmpty()

        var showMenu by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("HC Webhook") },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(context, AboutActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Webhook Log") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(context, LogsActivity::class.java)
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Feedback") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hc-webhook.feedbackjar.com/"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                // Show floating button only if at least one permission is granted and there are missing permissions for enabled types
                if (hasAtLeastOnePermission && missingPermissionsForEnabled.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            try {
                                permissionLauncher.launch(missingPermissionsForEnabled)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Grant Permission"
                            )
                        },
                        text = { Text("Grant") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Permission Status
                if (hasPermissions == false) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Permissions Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Health Connect permissions are needed to read health data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    try {
                                        permissionLauncher.launch(HealthConnectManager.ALL_PERMISSIONS)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                } else if (hasPermissions == true) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Permissions Granted",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "App can read health data from Health Connect",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Data Types Selection
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDataTypesExpanded = !isDataTypesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Data Types to Sync", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Select which health data to send to webhooks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isDataTypesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isDataTypesExpanded) "Collapse" else "Expand"
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = isDataTypesExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                HealthDataType.entries.forEach { dataType ->
                                    val permission = HealthPermission.getReadPermission(dataType.recordClass)
                                    val isPermissionGranted = permission in grantedPermissionsSet

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .alpha(if (isPermissionGranted) 1f else 0.5f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dataType.displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Switch(
                                            checked = dataType in enabledDataTypes,
                                            onCheckedChange = { checked ->
                                                // If at least one permission is granted, allow switching without showing modal
                                                // Otherwise, show the permission modal (existing behavior)
                                                if (!isPermissionGranted && checked && !hasAtLeastOnePermission) {
                                                    selectedDataTypeForPermission = dataType
                                                    showPermissionModal = true
                                                } else {
                                                    enabledDataTypes = if (checked) {
                                                        enabledDataTypes + dataType
                                                    } else {
                                                        enabledDataTypes - dataType
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sync Interval
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sync Interval", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = syncInterval,
                            onValueChange = { syncInterval = it },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Webhook URLs
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Webhook URLs", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Current URLs
                        webhookUrls.forEachIndexed { index, url ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = {
                                    webhookUrls = webhookUrls.toMutableList().apply { removeAt(index) }
                                }) {
                                    Text("❌")
                                }
                            }
                        }

                        // Add new URL
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                label = { Text("New URL") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (newUrl.isNotBlank() && newUrl.startsWith("http")) {
                                    webhookUrls = webhookUrls + newUrl
                                    newUrl = ""
                                } else {
                                    Toast.makeText(context, "Please enter a valid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Add")
                            }
                        }
                    }
                }

                // Manual Sync
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
                                              try {
                                                  // Store the sync callback to retry after permission grant
                                                  val syncCallback: suspend () -> Unit = {
                                                      isSyncing = true
                                                      syncMessage = null
                                                      try {
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
                                                  
                                                  // Store callback and request permissions
                                                  activity.pendingSyncCallback = {
                                                      scope.launch {
                                                          syncCallback()
                                                      }
                                                  }
                                                  permissionLauncher.launch(HealthConnectManager.ALL_PERMISSIONS)
                                                  
                                                  isSyncing = false
                                                  syncMessage = "Requesting permissions..."
                                                  return@launch
                                              } catch (e: Exception) {
                                                  isSyncing = false
                                                  syncMessage = "Failed to request permissions: ${e.message}"
                                                  return@launch
                                              }
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
                            enabled = !isSyncing && webhookUrls.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
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

                // Permission Modal
                if (showPermissionModal && selectedDataTypeForPermission != null) {
                    AlertDialog(
                        onDismissRequest = { showPermissionModal = false },
                        title = { Text("Permission Required") },
                        text = {
                            Text(
                                "Health Connect permission is required to sync ${selectedDataTypeForPermission!!.displayName}. " +
                                "Please grant system permission to enable this data type."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val permission = HealthPermission.getReadPermission(
                                        selectedDataTypeForPermission!!.recordClass
                                    )
                                    permissionLauncher.launch(setOf(permission))
                                    showPermissionModal = false
                                }
                            ) {
                                Text("Grant Permission")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showPermissionModal = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Save Button - only show when there are changes
                AnimatedVisibility(
                    visible = hasChanges,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val interval = syncInterval.toIntOrNull()
                                    if (interval == null || interval < 15) {
                                        Toast.makeText(context, "Sync interval must be at least 15 minutes", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    if (webhookUrls.isEmpty()) {
                                        Toast.makeText(context, "Please add at least one webhook URL", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    if (enabledDataTypes.isEmpty()) {
                                        Toast.makeText(context, "Please enable at least one data type", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    // Save preferences
                                    preferencesManager.setSyncIntervalMinutes(interval)
                                    preferencesManager.setWebhookUrls(webhookUrls)
                                    preferencesManager.setEnabledDataTypes(enabledDataTypes)

                                    // Update WorkManager
                                    (application as HCWebhookApplication).scheduleSyncWork()

                                    // Reset initial values to current values after saving
                                    initialSyncInterval = interval
                                    initialWebhookUrls = webhookUrls
                                    initialEnabledDataTypes = enabledDataTypes
                                    
                                    Toast.makeText(context, "Configuration saved!", Toast.LENGTH_SHORT).show()

                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error saving configuration", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Configuration")
                    }
                }

                // Status
                Text(
                    "App will sync ${enabledDataTypes.size} data type(s) every $syncInterval minutes to ${webhookUrls.size} webhook(s)",
                    style = MaterialTheme.typography.bodySmall
                )
                }
            }
        }
    }
}