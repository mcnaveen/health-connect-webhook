package com.hcwebhook.app.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    // State
    var syncMode by remember { mutableStateOf(preferencesManager.getSyncMode()) }
    var syncInterval by remember { mutableStateOf(preferencesManager.getSyncIntervalMinutes().toString()) }
    var scheduledSyncs by remember { mutableStateOf(preferencesManager.getScheduledSyncs()) }
    var enabledDataTypes by remember { mutableStateOf(preferencesManager.getEnabledDataTypes()) }
    
    var hasPermissions by remember { mutableStateOf<Boolean?>(null) }
    var grantedPermissionsSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showPermissionModal by remember { mutableStateOf(false) }
    var selectedDataTypeForPermission by remember { mutableStateOf<HealthDataType?>(null) }
    var isDataTypesExpanded by remember { mutableStateOf(false) }

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

    // Check permissions
    LaunchedEffect(Unit) {
        activity.permissionStatusCallback = { granted ->
            hasPermissions = granted
            if (granted) {
                scope.launch {
                    try {
                        val healthConnectManager = HealthConnectManager(context)
                        grantedPermissionsSet = healthConnectManager.getGrantedPermissions()
                    } catch (e: Exception) { }
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

     // Calculate missing permissions for enabled data types
    val missingPermissionsForEnabled = remember(enabledDataTypes, grantedPermissionsSet) {
        enabledDataTypes.mapNotNull { dataType ->
            val permission = HealthPermission.getReadPermission(dataType.recordClass)
            if (permission !in grantedPermissionsSet) permission else null
        }.toSet()
    }
    val hasAtLeastOnePermission = grantedPermissionsSet.isNotEmpty()

    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (hasAtLeastOnePermission && missingPermissionsForEnabled.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        try {
                            permissionLauncher.launch(missingPermissionsForEnabled)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    icon = { Icon(Icons.Filled.Shield, "Grant Permission") },
                    text = { Text("Grant") }
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
             if (hasPermissions == false) {
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
                                    text = "Permissions Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Health Connect permissions are needed to read health data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            try { permissionLauncher.launch(HealthConnectManager.ALL_PERMISSIONS) } 
                            catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant Permissions")
                        }
                    }
                }
            } else if (hasPermissions == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
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
                        Column {
                            Text(
                                text = "Permissions Granted",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "App can read health data from Health Connect",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Data Types Selection
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isDataTypesExpanded = !isDataTypesExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Data Types", style = MaterialTheme.typography.titleMedium)
                            Text("Select which health data to sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (isDataTypesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (isDataTypesExpanded) "Collapse" else "Expand")
                    }
                    
                    AnimatedVisibility(visible = isDataTypesExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HealthDataType.entries.forEach { dataType ->
                                val permission = HealthPermission.getReadPermission(dataType.recordClass)
                                val isPermissionGranted = permission in grantedPermissionsSet
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (isPermissionGranted) 1f else 0.5f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = dataType.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = dataType in enabledDataTypes,
                                        onCheckedChange = { checked ->
                                            if (!isPermissionGranted && checked && !hasAtLeastOnePermission) {
                                                selectedDataTypeForPermission = dataType
                                                showPermissionModal = true
                                            } else {
                                                val newSet = if (checked) enabledDataTypes + dataType else enabledDataTypes - dataType
                                                enabledDataTypes = newSet
                                                preferencesManager.setEnabledDataTypes(newSet)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sync Strategy Strategy
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

        // Permission Modal
        if (showPermissionModal && selectedDataTypeForPermission != null) {
            AlertDialog(
                onDismissRequest = { showPermissionModal = false },
                title = { Text("Permission Required") },
                text = { Text("Health Connect permission is required to sync ${selectedDataTypeForPermission!!.displayName}. Please grant system permission.") },
                confirmButton = {
                    Button(onClick = {
                        val permission = HealthPermission.getReadPermission(selectedDataTypeForPermission!!.recordClass)
                        permissionLauncher.launch(setOf(permission))
                        showPermissionModal = false
                    }) { Text("Grant Permission") }
                },
                dismissButton = { Button(onClick = { showPermissionModal = false }) { Text("Cancel") } }
            )
        }
    }
}