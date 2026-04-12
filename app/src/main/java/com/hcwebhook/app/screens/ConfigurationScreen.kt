package com.hcwebhook.app.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import com.hcwebhook.app.*
import com.hcwebhook.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    activity: MainActivity,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>,
    hasPermissions: Boolean?,
    grantedPermissionsSet: Set<String>,
    sdkStatus: Int,
    onPermissionsUpdated: (Boolean, Set<String>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    // State
    var syncMode by remember { mutableStateOf(preferencesManager.getSyncMode()) }
    var syncInterval by remember { mutableStateOf(preferencesManager.getSyncIntervalMinutes().toString()) }
    var scheduledSyncs by remember { mutableStateOf(preferencesManager.getScheduledSyncs()) }
    var enabledDataTypes by remember { mutableStateOf(preferencesManager.getEnabledDataTypes()) }

    var showPermissionModal by remember { mutableStateOf(false) }
    var selectedDataTypeForPermission by remember { mutableStateOf<HealthDataType?>(null) }
    var showDataTypesSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }

    // Last sync status
    var lastSyncTime by remember { mutableStateOf(preferencesManager.getLastSyncTime()) }
    var lastSyncSummary by remember { mutableStateOf(preferencesManager.getLastSyncSummary()) }
    var lastSyncRelativeTime by remember { mutableStateOf("") }

    // Refresh last sync relative time every 30 seconds
    LaunchedEffect(lastSyncTime) {
        while (true) {
            val syncTime = lastSyncTime
            lastSyncRelativeTime = if (syncTime != null) {
                val elapsed = System.currentTimeMillis() - syncTime
                val seconds = elapsed / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                when {
                    seconds < 60 -> "just now"
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    else -> "${hours / 24}d ago"
                }
            } else ""
            kotlinx.coroutines.delay(30_000)
        }
    }

    // No auto-enable logic. Users must explicitly toggle data types to enable them.
    // This resolves Issue #12 where data types were forcefully re-enabled on cold start.

    // Calculate missing permissions for enabled data types + ALWAYS require HISTORY permission
    val missingPermissionsForEnabled = remember(enabledDataTypes, grantedPermissionsSet) {
        val baseMissing = enabledDataTypes.mapNotNull { dataType ->
            val permission = HealthPermission.getReadPermission(dataType.recordClass)
            if (permission !in grantedPermissionsSet) permission else null
        }.toMutableSet()
        
        if (baseMissing.isNotEmpty() && "android.permission.health.READ_HEALTH_DATA_HISTORY" !in grantedPermissionsSet) {
            baseMissing.add("android.permission.health.READ_HEALTH_DATA_HISTORY")
        }
        baseMissing.toSet()
    }
    val hasAtLeastOnePermission = grantedPermissionsSet.isNotEmpty()
    val isBackgroundGranted = HealthConnectManager.BACKGROUND_PERMISSION_STR in grantedPermissionsSet

    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (missingPermissionsForEnabled.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        try {
                            permissionLauncher.launch(missingPermissionsForEnabled)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    icon = { Icon(Icons.Filled.Shield, "Grant Permission") },
                    text = { Text("Grant") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions Card
            if (hasPermissions == null) {
                // Still loading — stable-size placeholder
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Checking permissions…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                 Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             verticalAlignment = Alignment.CenterVertically,
                             horizontalArrangement = Arrangement.spacedBy(16.dp)
                         ) {
                             Icon(
                                 imageVector = Icons.Filled.Close,
                                 contentDescription = null,
                                 tint = MaterialTheme.colorScheme.onErrorContainer,
                                 modifier = Modifier.size(32.dp)
                             )
                             Column {
                                 Text(
                                     text = "Health Connect Not Found",
                                     style = MaterialTheme.typography.titleMedium,
                                     color = MaterialTheme.colorScheme.onErrorContainer
                                 )
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Text(
                                     text = "Health Connect is required to sync your health data with this app.",
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = MaterialTheme.colorScheme.onErrorContainer
                                 )
                             }
                         }
                         Spacer(modifier = Modifier.height(16.dp))
                         Button(onClick = {
                             val intent = Intent(Intent.ACTION_VIEW).apply {
                                 data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                 setPackage("com.android.vending")
                             }
                             // Fallback if Play Store is not installed
                             if (intent.resolveActivity(context.packageManager) != null) {
                                 context.startActivity(intent)
                             } else {
                                 val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                     data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                 }
                                 context.startActivity(webIntent)
                             }

                         }, modifier = Modifier.fillMaxWidth()) {
                             Text("Install Health Connect")
                         }
                     }
                 }
            } else if (hasPermissions == false) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "0 Permissions Granted",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To sync your health data, you must grant read access. The data is only read locally and sent directly to your configured URLs.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // We use strict data minimization: we no longer ask for INITIAL_PERMISSIONS here.
                        // The user must go to Data Types and select the types they want first.
                        Button(onClick = {
                            showDataTypesSheet = true
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Select Data Types Configure")
                        }
                    }
                }
            } else if (hasPermissions == true) {
                val totalPermCount = HealthDataType.entries.size
                val grantedPermCount = HealthDataType.entries.count { type ->
                    androidx.health.connect.client.permission.HealthPermission.getReadPermission(type.recordClass) in grantedPermissionsSet
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPermissionsSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permissions Granted",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$grantedPermCount of $totalPermCount permissions granted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "View permissions",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Data Types Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDataTypesSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Data Types",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${enabledDataTypes.size} items selected to sync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Select Data Types",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sync Strategy Strategy
            if (isBackgroundGranted) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sync Schedule", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Toggle Mode
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = syncMode == SyncMode.INTERVAL,
                            onClick = { 
                                syncMode = SyncMode.INTERVAL
                                preferencesManager.setSyncMode(SyncMode.INTERVAL)
                                // Reschedule
                                val app = activity.application as? HCWebhookApplication
                                app?.scheduleSyncWork()
                            },
                            label = { Text("Interval") },
                            leadingIcon = if (syncMode == SyncMode.INTERVAL) {
                                { Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = syncMode == SyncMode.SCHEDULED,
                            onClick = { 
                                syncMode = SyncMode.SCHEDULED
                                preferencesManager.setSyncMode(SyncMode.SCHEDULED)
                                // Reschedule
                                val app = activity.application as? HCWebhookApplication
                                app?.cancelSyncWork() // Stop interval work
                                ScheduledSyncManager(context).scheduleAllAlarms()
                            },
                            label = { Text("Scheduled") },
                            leadingIcon = if (syncMode == SyncMode.SCHEDULED) { 
                                { Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = syncMode == SyncMode.INTERVAL) {
                        Column {
                            OutlinedTextField(
                                value = syncInterval,
                                onValueChange = { 
                                    syncInterval = it
                                },
                                label = { Text("Interval (Minutes)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                             Button(
                                onClick = {
                                    val interval = syncInterval.toIntOrNull()
                                    if (interval != null && interval >= 15) {
                                        preferencesManager.setSyncIntervalMinutes(interval)
                                        val app = activity.application as? HCWebhookApplication
                                        app?.scheduleSyncWork()
                                        Toast.makeText(context, "Interval saved", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Min 15 minutes", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Update Interval")
                            }
                        }
                    }

                    AnimatedVisibility(visible = syncMode == SyncMode.SCHEDULED) {
                        Column {
                            scheduledSyncs.forEach { schedule ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = schedule.getDisplayTime(), // We might need to format this better or use helper
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = schedule.enabled,
                                            onCheckedChange = { enabled ->
                                                val updatedList = scheduledSyncs.map { 
                                                    if (it.id == schedule.id) it.copy(enabled = enabled) else it 
                                                }
                                                scheduledSyncs = updatedList
                                                preferencesManager.setScheduledSyncs(updatedList)
                                                // Update alarms
                                                val syncManager = ScheduledSyncManager(context)
                                                if (enabled) syncManager.scheduleAlarm(schedule) else syncManager.cancelAlarm(schedule.id)
                                            }
                                        )
                                        IconButton(onClick = {
                                            val updatedList = scheduledSyncs.filter { it.id != schedule.id }
                                            scheduledSyncs = updatedList
                                            preferencesManager.setScheduledSyncs(updatedList)
                                            ScheduledSyncManager(context).cancelAlarm(schedule.id)
                                        }) {
                                            Icon(Icons.Filled.Delete, "Delete")
                                        }
                                    }
                                }
                                Divider()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val newSchedule = ScheduledSync.create(hour, minute)
                                            val updatedList = scheduledSyncs + newSchedule
                                            scheduledSyncs = updatedList
                                            preferencesManager.setScheduledSyncs(updatedList)
                                            ScheduledSyncManager(context).scheduleAlarm(newSchedule)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Schedule Time")
                            }
                        }
                    }
                }
            }
            } else if (hasPermissions == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Background Permission Required", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("To sync data automatically on a schedule or interval, you must grant Health Connect access for background use.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            try {
                                permissionLauncher.launch(setOf(HealthConnectManager.BACKGROUND_PERMISSION_STR))
                            } catch(e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("Grant Background Permission")
                        }
                    }
                }
            }

            // Last Sync Status
            if (lastSyncTime != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Last Sync",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                lastSyncRelativeTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lastSyncSummary != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                lastSyncSummary!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Manual Sync
            com.hcwebhook.app.components.ManualSyncCard(onSyncCompleted = {
                 lastSyncTime = preferencesManager.getLastSyncTime()
                 lastSyncSummary = preferencesManager.getLastSyncSummary()
            })
        }

        // Permissions bottom sheet
        if (showPermissionsSheet) {
            PermissionsBottomSheet(
                grantedPermissionsSet = grantedPermissionsSet,
                onDismiss = { showPermissionsSheet = false }
            )
        }

        // Data Types bottom sheet
        if (showDataTypesSheet) {
            DataTypesBottomSheet(
                enabledDataTypes = enabledDataTypes,
                grantedPermissionsSet = grantedPermissionsSet,
                missingPermissionsForEnabled = missingPermissionsForEnabled,
                onDismiss = { showDataTypesSheet = false },
                onToggleDataType = { dataType, checked ->
                    val newSet = if (checked) enabledDataTypes + dataType else enabledDataTypes - dataType
                    enabledDataTypes = newSet
                    preferencesManager.setEnabledDataTypes(newSet)
                },
                onRequestPermissions = {
                    try {
                        permissionLauncher.launch(missingPermissionsForEnabled)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    showDataTypesSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTypesBottomSheet(
    enabledDataTypes: Set<HealthDataType>,
    grantedPermissionsSet: Set<String>,
    missingPermissionsForEnabled: Set<String>,
    onDismiss: () -> Unit,
    onToggleDataType: (HealthDataType, Boolean) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Types",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${enabledDataTypes.size} items selected to sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rationale required by Google Play
            Text(
                text = "Health Connect read permissions are required to access this data locally and automatically sync it to your configured webhooks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Data Types list
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(HealthDataType.entries) { dataType ->
                    val isPermissionGranted = HealthPermission.getReadPermission(dataType.recordClass) in grantedPermissionsSet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .alpha(if (isPermissionGranted) 1f else 0.5f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = dataType.displayName, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = dataType in enabledDataTypes,
                            onCheckedChange = { checked ->
                                onToggleDataType(dataType, checked)
                            }
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            if (missingPermissionsForEnabled.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Missing Permissions")
                }
            }
        }
    }
}
