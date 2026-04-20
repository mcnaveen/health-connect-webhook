package com.hcwebhook.app.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.hcwebhook.app.HealthConnectManager
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.SyncManager
import com.hcwebhook.app.SyncResult
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.compose.ui.res.stringResource
import com.hcwebhook.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSyncCard(onSyncCompleted: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmSheet by remember { mutableStateOf(false) }
    
    val webhookConfigs = preferencesManager.getWebhookConfigs()

    val timeRangeOptions = listOf(
        context.getString(R.string.manual_sync_default_range) to null,
        context.getString(R.string.manual_sync_past_1_day) to 1,
        context.getString(R.string.manual_sync_past_7_days) to 7,
        context.getString(R.string.manual_sync_past_30_days) to 30,
        context.getString(R.string.manual_sync_custom) to -1
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionIndex by remember { mutableStateOf(0) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // ── Confirmation Bottom Sheet ──────────────────────────────────────────────
    if (showConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfirmSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.manual_sync_confirm_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.manual_sync_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        showConfirmSheet = false
                        if (isSyncing) return@Button

                        scope.launch {
                            isSyncing = true
                            syncMessage = null

                            try {
                                val availability = HealthConnectClient.getSdkStatus(context)
                                if (availability != HealthConnectClient.SDK_AVAILABLE) {
                                    syncMessage = when (availability) {
                                        HealthConnectClient.SDK_UNAVAILABLE -> context.getString(R.string.err_hc_not_installed)
                                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> context.getString(R.string.err_hc_needs_update)
                                        else -> context.getString(R.string.err_hc_not_available)
                                    }
                                    isSyncing = false
                                    return@launch
                                }

                                val healthConnectManager = HealthConnectManager(context)
                                val enabledTypes = preferencesManager.getEnabledDataTypes()
                                val requiredPermissions = HealthConnectManager.getPermissionsForTypes(
                                    enabledTypes,
                                    includeBackgroundPermission = false
                                )
                                if (requiredPermissions.isNotEmpty() && !healthConnectManager.hasPermissions(requiredPermissions)) {
                                    syncMessage = context.getString(R.string.err_permissions_required)
                                    isSyncing = false
                                    return@launch
                                }

                                val syncManager = SyncManager(context)
                                val timeRangeSelection = timeRangeOptions[selectedOptionIndex].second

                                val result = if (timeRangeSelection == -1) {
                                    // custom date range
                                    val startInstant = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
                                    // normalize both boundaries to midnight UTC
                                    val endInstant = endDate?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
                                    if (startInstant == null || endInstant == null) {
                                        syncMessage = context.getString(R.string.err_select_both_dates)
                                        isSyncing = false
                                        return@launch
                                    }

                                    syncMessage = context.getString(R.string.manual_sync_progress, startDate.toString(), endDate.toString())
                                    syncManager.performSync(start = startInstant, end = endInstant)
                                } else {
                                    // sync the last N days, or from the last sync
                                    syncManager.performSync(timeRangeSelection)
                                }

                                when {
                                    result.isSuccess -> {
                                        val syncResult = result.getOrThrow()
                                        syncMessage = when (syncResult) {
                                            is SyncResult.NoData -> context.getString(R.string.manual_sync_no_data)
                                            is SyncResult.Success -> {
                                                val parts = syncResult.syncCounts.map { (type, count) ->
                                                    "$count ${context.getString(type.nameResId).lowercase()}"
                                                }
                                                if (parts.isEmpty()) context.getString(R.string.manual_sync_success_empty)
                                                else context.getString(R.string.manual_sync_success_items, parts.joinToString(", "))
                                            }
                                        }
                                        onSyncCompleted()
                                    }
                                    result.isFailure -> {
                                        syncMessage = context.getString(R.string.manual_sync_failed, result.exceptionOrNull()?.message ?: "Unknown error")
                                    }
                                }
                            } catch (e: Exception) {
                                syncMessage = context.getString(R.string.manual_sync_failed, e.message)
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manual_sync_btn))
                }
                OutlinedButton(
                    onClick = { showConfirmSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    // ── Card UI ───────────────────────────────────────────────────────────────
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.manual_sync_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.manual_sync_desc),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = timeRangeOptions[selectedOptionIndex].first,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.manual_sync_time_range)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    timeRangeOptions.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option.first) },
                            onClick = {
                                selectedOptionIndex = index
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedOptionIndex == timeRangeOptions.indexOfFirst { it.second == -1 }) {
                val today = LocalDate.now()
                val todayMillis = remember(today) {
                    today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }

                if (showStartDatePicker) {
                    val startPickerState = rememberDatePickerState(
                        initialSelectedDateMillis = startDate
                            ?.atStartOfDay(ZoneOffset.UTC)
                            ?.toInstant()
                            ?.toEpochMilli()
                            ?: todayMillis,
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return utcTimeMillis <= todayMillis
                            }

                            override fun isSelectableYear(year: Int): Boolean {
                                return year <= today.year
                            }
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selectedMillis = startPickerState.selectedDateMillis
                                startDate = selectedMillis?.let {
                                    Instant.ofEpochMilli(it)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                }
                                showStartDatePicker = false
                            }) {
                                Text(stringResource(R.string.action_ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartDatePicker = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    ) {
                        DatePicker(state = startPickerState)
                    }
                }

                if (showEndDatePicker) {
                    val currentEndDate = endDate
                    val minEndDateMillis = startDate
                        ?.atStartOfDay(ZoneOffset.UTC)
                        ?.toInstant()
                        ?.toEpochMilli()
                    val minEndYear = startDate?.year
                    val endPickerState = rememberDatePickerState(
                        initialSelectedDateMillis = when {
                            currentEndDate != null && minEndDateMillis != null -> {
                                val endDateMillis = currentEndDate
                                    .atStartOfDay(ZoneOffset.UTC)
                                    .toInstant()
                                    .toEpochMilli()
                                maxOf(endDateMillis, minEndDateMillis)
                            }
                            currentEndDate != null -> currentEndDate
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                            minEndDateMillis != null -> minEndDateMillis
                            else -> todayMillis
                        },
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return utcTimeMillis <= todayMillis &&
                                    (minEndDateMillis == null || utcTimeMillis >= minEndDateMillis)
                            }

                            override fun isSelectableYear(year: Int): Boolean {
                                return year <= today.year && (minEndYear == null || year >= minEndYear)
                            }
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selectedMillis = endPickerState.selectedDateMillis
                                endDate = selectedMillis?.let {
                                    Instant.ofEpochMilli(it)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                }
                                showEndDatePicker = false
                            }) {
                                Text(stringResource(R.string.action_ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDatePicker = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    ) {
                        DatePicker(state = endPickerState)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(startDate?.let { context.getString(R.string.manual_sync_start_label, it.toString()) } ?: stringResource(R.string.manual_sync_start_date))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(endDate?.let { context.getString(R.string.manual_sync_end_label, it.toString()) } ?: stringResource(R.string.manual_sync_end_date))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val startDateParsed = startDate
                    val endDateParsed = endDate

                    if (startDateParsed != null && endDateParsed != null) {
                        when {
                            endDateParsed < startDateParsed -> {
                                Text(
                                    stringResource(R.string.err_end_date_before_start),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            endDateParsed > today -> {
                                Text(
                                    stringResource(R.string.err_end_date_future),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showConfirmSheet = true },
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
                    Text(stringResource(R.string.manual_sync_syncing))
                } else {
                    Text(stringResource(R.string.manual_sync_btn))
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
