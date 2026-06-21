package com.hcwebhook.app.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    activity: MainActivity,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>,
    hasPermissions: Boolean?,
    grantedPermissionsSet: Set<String>,
    sdkStatus: Int,
    onOpenLocalHttpSettings: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER")
    onPermissionsUpdated: (Boolean, Set<String>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var syncMode by remember { mutableStateOf(preferencesManager.getSyncMode()) }
    var syncInterval by remember { mutableStateOf(preferencesManager.getSyncIntervalMinutes().toString()) }
    var scheduledSyncs by remember { mutableStateOf(preferencesManager.getScheduledSyncs()) }
    var enabledDataTypes by remember { mutableStateOf(preferencesManager.getEnabledDataTypes()) }

    // Battery optimization banner state
    var isBatteryOptimized by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    var batteryBannerDismissed by remember { mutableStateOf(preferencesManager.isBatteryBannerDismissed()) }
    val showBatteryBanner = isBatteryOptimized && !batteryBannerDismissed

    // Re-check battery optimization status every time the user returns to the app
    // (e.g. after granting the exemption in the system dialog).
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val oemDeepLinkIntent = remember { BatteryOptimizationHelper.buildOemDeepLinkIntent() }
    val oemLabel = remember { BatteryOptimizationHelper.oemDeepLinkLabel() }

    var showDataTypesSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }

    var lastSyncTime by remember { mutableStateOf(preferencesManager.getLastSyncTime()) }
    var lastSyncSummary by remember { mutableStateOf(preferencesManager.getLastSyncSummary()) }
    var lastSyncRelativeTime by remember { mutableStateOf("") }
    val isLocalHttpEnabled = preferencesManager.isLocalTcpEnabled()

    val reliabilityStat = remember {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val logs = preferencesManager.getWebhookLogs()
            .filter { it.timestamp >= cutoff && it.syncType != "test" }
        if (logs.isEmpty()) null
        else "${logs.count { it.success }}/${logs.size}"
    }
    val reliabilityAllOk = remember {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        preferencesManager.getWebhookLogs()
            .filter { it.timestamp >= cutoff && it.syncType != "test" }
            .all { it.success }
    }
    val localHttpPort = preferencesManager.getLocalTcpPort()

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
        // Write-back grants ride along with the normal permission request.
        baseMissing.addAll(HealthConnectManager.WRITE_PERMISSIONS.filter { it !in grantedPermissionsSet })
        baseMissing.toSet()
    }
    val isBackgroundGranted = HealthConnectManager.BACKGROUND_PERMISSION_STR in grantedPermissionsSet

    val scrollState = rememberScrollState()
    val statusDotTransition = rememberInfiniteTransition(label = "local_http_status_dot")
    val statusDotAlpha by statusDotTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "local_http_status_dot_alpha"
    )

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── App header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Health Connect Webhook",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (lastSyncTime != null) {
                        Text(
                            text = "Last sync $lastSyncRelativeTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (reliabilityStat != null) {
                        Text(
                            text = "24h: $reliabilityStat ok",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reliabilityAllOk) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onOpenDashboard,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = stringResource(R.string.dashboard_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (lastSyncSummary != null) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ── Local HTTP server banner ──────────────────────────────────────
            if (isLocalHttpEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLocalHttpSettings() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(statusDotAlpha)
                                    .background(Color(0xFF22C55E), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.config_local_http_status_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.config_local_http_status_desc, localHttpPort),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenLocalHttpSettings() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.config_local_tcp_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Battery optimization banner ────────────────────────────────────
            if (showBatteryBanner) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.battery_banner_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.battery_banner_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Primary action: standard Android system dialog
                        Button(
                            onClick = {
                                // Try each option in order. Do NOT use resolveActivity() —
                                // on Android 11+ it returns null for system intents due to
                                // package visibility filtering, even when they ARE resolvable.
                                // Instead call startActivity() directly and fall through on failure.
                                var opened = false

                                // Option 1: Standard Android system dialog
                                if (!opened) {
                                    try {
                                        context.startActivity(
                                            BatteryOptimizationHelper.buildRequestExemptionIntent(context)
                                        )
                                        opened = true
                                    } catch (_: Exception) {}
                                }

                                // Option 2: OEM-specific screen (Samsung Device Care, etc.)
                                if (!opened) {
                                    val oemIntent = BatteryOptimizationHelper.buildOemDeepLinkIntent()
                                    if (oemIntent != null) {
                                        try {
                                            context.startActivity(oemIntent)
                                            opened = true
                                        } catch (_: Exception) {}
                                    }
                                }

                                // Option 3: Generic battery optimization settings list
                                if (!opened) {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                        opened = true
                                    } catch (_: Exception) {}
                                }

                                if (!opened) {
                                    Toast.makeText(
                                        context,
                                        "Could not open battery settings. Please disable battery optimization manually in your device Settings.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.battery_banner_action_fix))
                        }
                        // OEM-specific deep-link (Samsung, Xiaomi, etc.)
                        if (oemDeepLinkIntent != null && oemLabel != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(oemDeepLinkIntent)
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(
                                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.config_toast_error, e.message), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.battery_banner_action_oem, oemLabel),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Dismiss — user can re-open via system settings themselves
                        TextButton(
                            onClick = {
                                batteryBannerDismissed = true
                                preferencesManager.setBatteryBannerDismissed(true)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(R.string.battery_banner_action_dismiss),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // ── Battery optimization hint (after banner dismissed) ─────────────
            if (isBatteryOptimized && batteryBannerDismissed) {
                TextButton(
                    onClick = {
                        batteryBannerDismissed = false
                        preferencesManager.setBatteryBannerDismissed(false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.battery_banner_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── Permission status ─────────────────────────────────────────────
            when {
                hasPermissions == null -> {
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.config_perms_checking), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                sdkStatus != HealthConnectClient.SDK_AVAILABLE -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.config_hc_not_found_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(stringResource(R.string.config_hc_not_found_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
                hasPermissions == false -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(R.string.config_perms_zero_title), style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(stringResource(R.string.config_perms_zero_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showDataTypesSheet = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.config_action_select_datatypes))
                            }
                        }
                    }
                }
                hasPermissions == true -> {
                    val grantedPermCount = HealthDataType.entries.count { type ->
                        HealthPermission.getReadPermission(type.recordClass) in grantedPermissionsSet
                    }
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { showPermissionsSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.config_permissions_granted), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(id = R.string.datatypes_granted_summary, grantedPermCount, HealthDataType.entries.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ── Data types ────────────────────────────────────────────────────
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { showDataTypesSheet = true }
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.config_data_types), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(id = R.string.datatypes_selected_summary, enabledDataTypes.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            // ── Sync schedule ─────────────────────────────────────────────────
            if (isBackgroundGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.config_sync_schedule_title), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedVisibility(visible = syncMode == SyncMode.INTERVAL, enter = expandVertically(), exit = shrinkVertically()) {
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
                        AnimatedVisibility(visible = syncMode == SyncMode.SCHEDULED, enter = expandVertically(), exit = shrinkVertically()) {
                            Column {
                                // Exact alarm permission warning
                                if (!ScheduledSyncManager(context).canScheduleExactAlarms()) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clickable {
                                                try {
                                                    context.startActivity(
                                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    try {
                                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                        })
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.exact_alarm_warning),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                                scheduledSyncs.forEach { schedule ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = schedule.getDisplayTime(), style = MaterialTheme.typography.bodyMedium)
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
                                            }) { Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(18.dp)) }
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    val calendar = Calendar.getInstance()
                                    TimePickerDialog(context, { _, hour, minute ->
                                        val newSchedule = ScheduledSync.create(hour, minute)
                                        val updatedList = scheduledSyncs + newSchedule
                                        scheduledSyncs = updatedList
                                        preferencesManager.setScheduledSyncs(updatedList)
                                        ScheduledSyncManager(context).scheduleAlarm(newSchedule)
                                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.config_action_add_schedule))
                                }
                            }
                        }
                    }
                }
            } else if (hasPermissions == true && !isBackgroundGranted) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.config_bg_perm_title), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(stringResource(R.string.config_bg_perm_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = {
                            try { permissionLauncher.launch(setOf(HealthConnectManager.BACKGROUND_PERMISSION_STR)) } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.config_action_grant_bg))
                        }
                    }
                }
            }

            // ── Manual sync ───────────────────────────────────────────────────
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

@Composable
private fun resolutionChipOptions(family: DataResolutionFamily): List<Pair<Int, String>> {
    val full = stringResource(R.string.config_hr_resolution_full)
    val oneMin = stringResource(R.string.config_hr_resolution_1m)
    val fiveMin = stringResource(R.string.config_hr_resolution_5m)
    val fifteenMin = stringResource(R.string.config_hr_resolution_15m)
    val daily = stringResource(R.string.config_steps_resolution_daily)
    val sleepFull = stringResource(R.string.config_sleep_resolution_full)
    val sleepSummary = stringResource(R.string.config_sleep_resolution_summary)
    val intervalBuckets = listOf(
        1 to oneMin,
        5 to fiveMin,
        15 to fifteenMin,
    )
    return when (family) {
        DataResolutionFamily.INTERVAL_WITH_DAILY -> listOf(
            RESOLUTION_DAILY to daily,
            RESOLUTION_FULL to full,
        ) + intervalBuckets
        DataResolutionFamily.INTERVAL_RAW_DEFAULT -> listOf(
            RESOLUTION_FULL to full,
            RESOLUTION_DAILY to daily,
        ) + intervalBuckets
        DataResolutionFamily.SAMPLE_SERIES -> listOf(RESOLUTION_FULL to full) + intervalBuckets
        DataResolutionFamily.SLEEP_SESSION -> listOf(
            RESOLUTION_FULL to sleepFull,
            SLEEP_SUMMARY to sleepSummary,
        )
    }
}

/**
 * Nested resolution picker shown under an enabled data type in the sheet.
 * [options] maps each stored value to its label; [selected] is the current value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResolutionChipRow(
    subtitle: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 4.dp, top = 2.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.config_resolution_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DataTypesBottomSheet(
    enabledDataTypes: Set<HealthDataType>,
    grantedPermissionsSet: Set<String>,
    missingPermissionsForEnabled: Set<String>,
    onDismiss: () -> Unit,
    onToggleDataType: (HealthDataType, Boolean) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var resolutions by remember { mutableStateOf(preferencesManager.getDataTypeResolutions()) }
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
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(HealthDataType.entries) { dataType ->
                    val isPermissionGranted = HealthPermission.getReadPermission(dataType.recordClass) in grantedPermissionsSet
                    val isEnabled = dataType in enabledDataTypes
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isPermissionGranted) 1f else 0.5f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = iconForDataType(dataType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = stringResource(id = dataType.nameResId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = stringResource(id = dataType.rationaleResId),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked -> onToggleDataType(dataType, checked) },
                            )
                        }
                        val resolutionFamily = dataType.resolutionFamily
                        if (resolutionFamily != null) {
                            AnimatedVisibility(
                                visible = isEnabled,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                ResolutionChipRow(
                                    subtitle = stringResource(resolutionSubtitleResId(resolutionFamily)),
                                    options = resolutionChipOptions(resolutionFamily),
                                    selected = resolutions[dataType] ?: dataType.defaultResolutionMinutes,
                                    onSelect = {
                                        resolutions = resolutions + (dataType to it)
                                        preferencesManager.setDataTypeResolution(dataType, it)
                                    },
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
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
