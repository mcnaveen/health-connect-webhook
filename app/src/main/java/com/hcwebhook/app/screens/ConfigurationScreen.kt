package com.hcwebhook.app.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import com.hcwebhook.app.*
import com.hcwebhook.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.hcwebhook.app.R

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

    var syncMode by remember { mutableStateOf(preferencesManager.getSyncMode()) }
    var syncInterval by remember { mutableStateOf(preferencesManager.getSyncIntervalMinutes().toString()) }
    var scheduledSyncs by remember { mutableStateOf(preferencesManager.getScheduledSyncs()) }
    var enabledDataTypes by remember { mutableStateOf(preferencesManager.getEnabledDataTypes()) }

    var showDataTypesSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }

    var lastSyncTime by remember { mutableStateOf(preferencesManager.getLastSyncTime()) }
    var lastSyncSummary by remember { mutableStateOf(preferencesManager.getLastSyncSummary()) }
    var lastSyncRelativeTime by remember { mutableStateOf("") }

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
                    text = { Text(stringResource(R.string.config_action_grant)) },
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
            if (hasPermissions == null) {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.config_perms_checking), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
                 OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.config_hc_not_found_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(stringResource(R.string.config_hc_not_found_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            } else if (hasPermissions == false) {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.config_perms_zero_title), fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.config_perms_zero_desc), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Button(onClick = { showDataTypesSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.config_action_select_datatypes))
                }
            } else if (hasPermissions == true) {
                val grantedPermCount = HealthDataType.entries.count { type ->
                    HealthPermission.getReadPermission(type.recordClass) in grantedPermissionsSet
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().clickable { showPermissionsSheet = true }
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.config_permissions_granted), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(stringResource(id = R.string.datatypes_granted_summary, grantedPermCount, HealthDataType.entries.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        TextButton(onClick = { showPermissionsSheet = true }) {
                            Text(stringResource(R.string.config_action_view_perms))
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().clickable { showDataTypesSheet = true }
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.config_data_types), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(id = R.string.datatypes_selected_summary, enabledDataTypes.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (isBackgroundGranted) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.config_sync_schedule_title), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = syncMode == SyncMode.INTERVAL,
                                onClick = { syncMode = SyncMode.INTERVAL; preferencesManager.setSyncMode(SyncMode.INTERVAL); (activity.application as? HCWebhookApplication)?.scheduleSyncWork() },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text(stringResource(R.string.config_sync_mode_interval)) }
                            SegmentedButton(
                                selected = syncMode == SyncMode.SCHEDULED,
                                onClick = { syncMode = SyncMode.SCHEDULED; preferencesManager.setSyncMode(SyncMode.SCHEDULED); (activity.application as? HCWebhookApplication)?.cancelSyncWork(); ScheduledSyncManager(context).scheduleAllAlarms() },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text(stringResource(R.string.config_sync_mode_scheduled)) }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedVisibility(visible = syncMode == SyncMode.INTERVAL) {
                            Column {
                                OutlinedTextField(
                                    value = syncInterval,
                                    onValueChange = { syncInterval = it },
                                    label = { Text(stringResource(R.string.config_sync_interval_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val interval = syncInterval.toIntOrNull()
                                        if (interval != null && interval >= 15) {
                                            preferencesManager.setSyncIntervalMinutes(interval)
                                            (activity.application as? HCWebhookApplication)?.scheduleSyncWork()
                                            Toast.makeText(context, context.getString(R.string.config_toast_interval_saved), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.config_toast_min_interval), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) { Text(stringResource(R.string.config_action_update_interval)) }
                            }
                        }

                        AnimatedVisibility(visible = syncMode == SyncMode.SCHEDULED) {
                            Column {
                                scheduledSyncs.forEach { schedule ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = schedule.getDisplayTime(), style = MaterialTheme.typography.titleMedium)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(checked = schedule.enabled, onCheckedChange = { enabled ->
                                                val updatedList = scheduledSyncs.map { if (it.id == schedule.id) it.copy(enabled = enabled) else it }
                                                scheduledSyncs = updatedList
                                                preferencesManager.setScheduledSyncs(updatedList)
                                                val syncManager = ScheduledSyncManager(context)
                                                if (enabled) syncManager.scheduleAlarm(schedule) else syncManager.cancelAlarm(schedule.id)
                                            })
                                            IconButton(onClick = {
                                                val updatedList = scheduledSyncs.filter { it.id != schedule.id }
                                                scheduledSyncs = updatedList
                                                preferencesManager.setScheduledSyncs(updatedList)
                                                ScheduledSyncManager(context).cancelAlarm(schedule.id)
                                            }) { Icon(Icons.Filled.Delete, "Delete") }
                                        }
                                    }
                                    HorizontalDivider()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        TimePickerDialog(context, { _, hour, minute ->
                                            val newSchedule = ScheduledSync.create(hour, minute)
                                            val updatedList = scheduledSyncs + newSchedule
                                            scheduledSyncs = updatedList
                                            preferencesManager.setScheduledSyncs(updatedList)
                                            ScheduledSyncManager(context).scheduleAlarm(newSchedule)
                                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Add, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.config_action_add_schedule))
                                }
                            }
                        }
                    }
                }
            } else if (hasPermissions == true) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.config_bg_perm_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.config_bg_perm_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            try { permissionLauncher.launch(setOf(HealthConnectManager.BACKGROUND_PERMISSION_STR)) } catch(e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(stringResource(R.string.config_action_grant_bg))
                        }
                    }
                }
            }

            if (lastSyncTime != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.config_last_sync_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastSyncRelativeTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (lastSyncSummary != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(lastSyncSummary!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            com.hcwebhook.app.components.ManualSyncCard(onSyncCompleted = {
                 lastSyncTime = preferencesManager.getLastSyncTime()
                 lastSyncSummary = preferencesManager.getLastSyncSummary()
            })
        }

        if (showPermissionsSheet) {
            PermissionsBottomSheet(grantedPermissionsSet = grantedPermissionsSet, onDismiss = { showPermissionsSheet = false })
        }

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
                    try { permissionLauncher.launch(missingPermissionsForEnabled) } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = stringResource(R.string.config_data_types), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(id = R.string.datatypes_selected_summary, enabledDataTypes.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.dt_hc_rationale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(HealthDataType.entries) { dataType ->
                    val isPermissionGranted = HealthPermission.getReadPermission(dataType.recordClass) in grantedPermissionsSet
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).alpha(if (isPermissionGranted) 1f else 0.5f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = iconForDataType(dataType), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = stringResource(id = dataType.nameResId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(text = stringResource(id = dataType.rationaleResId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = dataType in enabledDataTypes, onCheckedChange = { checked -> onToggleDataType(dataType, checked) })
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (missingPermissionsForEnabled.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Filled.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.config_action_grant_missing))
                }
            }
        }
    }
}
