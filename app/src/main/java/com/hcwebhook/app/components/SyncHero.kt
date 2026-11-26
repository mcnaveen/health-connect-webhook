package com.hcwebhook.app.components

import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hcwebhook.app.ui.theme.Mono
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.hcwebhook.app.HealthConnectManager
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import com.hcwebhook.app.SyncManager
import com.hcwebhook.app.SyncResult
import com.hcwebhook.app.WebhookConfig
import com.hcwebhook.app.hourlyBars
import com.hcwebhook.app.ui.SectionTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The status card at the top of Home: what the sync does, the last sync, the counts of the last
 * 24 hours, and the Sync now button. Sync now opens one sheet with the time range and the target.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SyncHero(
    title: String,
    subtitle: String,
    sentLast24h: Int?,
    failedLast24h: Int?,
    lastSyncTime: Long?,
    /** Webhook deliveries of the last 24 hours: time and success. */
    syncEvents: List<Pair<Long, Boolean>> = emptyList(),
    canSync: Boolean,
    onSyncCompleted: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmSheet by remember { mutableStateOf(false) }

    val webhookConfigs = preferencesManager.getWebhookConfigs()
    val enabledWebhooks = remember(webhookConfigs) { webhookConfigs.filter { it.isEnabled } }
    var selectedWebhookUrl by remember { mutableStateOf<String?>(null) }

    val timeRangeOptions = listOf(
        context.getString(R.string.manual_sync_default_range) to null,
        context.getString(R.string.manual_sync_past_1_day) to 1,
        context.getString(R.string.manual_sync_past_7_days) to 7,
        context.getString(R.string.manual_sync_past_30_days) to 30,
        context.getString(R.string.manual_sync_custom) to -1
    )
    val localDateSaver = Saver<LocalDate?, String>(
        save = { it?.toString() ?: "" },
        restore = { if (it.isEmpty()) null else LocalDate.parse(it) }
    )
    var selectedOptionIndex by rememberSaveable { mutableStateOf(0) }
    var startDate by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(null) }
    var endDate by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val isCustomRange = timeRangeOptions[selectedOptionIndex].second == -1
    val today = LocalDate.now()
    val selectedStart = startDate
    val selectedEnd = endDate
    val customRangeValid = !isCustomRange || (
        selectedStart != null && selectedEnd != null &&
            !selectedEnd.isBefore(selectedStart) && !selectedEnd.isAfter(today)
        )

    // Relative time of the last sync, localized by Android and refreshed every 30 seconds.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val lastSyncText = lastSyncTime?.let {
        DateUtils.getRelativeTimeSpanString(it, now, DateUtils.MINUTE_IN_MILLIS).toString()
    } ?: stringResource(R.string.home_hero_never)

    // ── Sync now sheet ───────────────────────────────────────────────────────
    if (showConfirmSheet) {
        ModalBottomSheet(onDismissRequest = { showConfirmSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.manual_sync_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Text(
                    stringResource(R.string.manual_sync_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                SectionTitle(stringResource(R.string.manual_sync_time_range), Modifier.padding(top = 8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    timeRangeOptions.forEachIndexed { index, option ->
                        FilterChip(
                            selected = index == selectedOptionIndex,
                            onClick = { selectedOptionIndex = index },
                            label = { Text(option.first) },
                        )
                    }
                }
                AnimatedVisibility(visible = isCustomRange, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp),
                    ) {
                        OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(
                                startDate?.toString() ?: stringResource(R.string.manual_sync_start_date),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(
                                endDate?.toString() ?: stringResource(R.string.manual_sync_end_date),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (isCustomRange && selectedStart != null && selectedEnd != null && selectedEnd < selectedStart) {
                    Text(
                        stringResource(R.string.err_end_date_before_start),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                if (enabledWebhooks.size >= 2) {
                    SectionTitle(stringResource(R.string.manual_sync_send_to), Modifier.padding(top = 8.dp))
                    Column {
                        (listOf<WebhookConfig?>(null) + enabledWebhooks).forEach { config ->
                            val label = config?.let {
                                remember(it.url) {
                                    try { java.net.URI(it.url).host?.takeIf { host -> host.isNotEmpty() } ?: it.url }
                                    catch (_: Exception) { it.url }
                                }
                            } ?: stringResource(R.string.manual_sync_all_webhooks)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWebhookUrl = config?.url },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedWebhookUrl == config?.url, onClick = { selectedWebhookUrl = config?.url })
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                            includeBackgroundPermission = false,
                            // The manual-sync gate should only require read access to the
                            // enabled data types. READ_HEALTH_DATA_HISTORY is only needed for
                            // ranges older than 30 days, and READ_STEPS_CADENCE is optional
                            // (its aggregate read is already try/catch-guarded). Requiring
                            // either here left users stuck on "Permissions required for sync"
                            // with no in-app way to grant them.
                            includeHistoryPermission = false,
                            includeStepsCadence = false
                        )
                        if (requiredPermissions.isNotEmpty() && !healthConnectManager.hasPermissions(requiredPermissions)) {
                            syncMessage = context.getString(R.string.err_permissions_required)
                            isSyncing = false
                            return@launch
                        }

                        val syncManager = SyncManager(context)
                        val timeRangeSelection = timeRangeOptions[selectedOptionIndex].second

                        val targetWebhooks: List<WebhookConfig>? = selectedWebhookUrl?.let { url ->
                            enabledWebhooks.filter { it.url == url }
                        }

                        val result = if (timeRangeSelection == -1) {
                            // Custom range: inclusive local calendar days
                            // [startDate 00:00, endDate+1 00:00) in the device zone.
                            val zone = ZoneId.systemDefault()
                            val startInstant = startDate?.atStartOfDay(zone)?.toInstant()
                            val endInstant = endDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
                            if (startInstant == null || endInstant == null) {
                                syncMessage = context.getString(R.string.err_select_both_dates)
                                isSyncing = false
                                return@launch
                            }
                            if (!startInstant.isBefore(endInstant)) {
                                syncMessage = context.getString(R.string.err_end_date_before_start)
                                isSyncing = false
                                return@launch
                            }

                            syncMessage = context.getString(R.string.manual_sync_progress, startDate.toString(), endDate.toString())
                            syncManager.performSync(start = startInstant, end = endInstant, syncType = "manual", targetWebhooks = targetWebhooks)
                        } else {
                            // sync the last N days, or from the last sync
                            syncManager.performSync(timeRangeSelection, syncType = "manual", targetWebhooks = targetWebhooks)
                        }

                        when {
                            result.isSuccess -> {
                                val syncResult = result.getOrThrow()
                                syncMessage = when (syncResult) {
                                    is SyncResult.NoData -> context.getString(R.string.manual_sync_no_data)
                                    is SyncResult.NoMatchingData -> context.getString(R.string.manual_sync_no_matching_data)
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        syncMessage = context.getString(R.string.manual_sync_failed, e.message)
                    } finally {
                        isSyncing = false
                    }
                }
                    },
                    enabled = customRangeValid,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.manual_sync_btn))
                }
            }
        }
    }

    if (isCustomRange) {
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
    }

    // ── Status card ──────────────────────────────────────────────────────────
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer.copy(alpha = 0.85f))
            Spacer(Modifier.height(16.dp))
            ActivityBars(events = syncEvents, now = now, active = isSyncing)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(stringResource(R.string.home_pulse_start), style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.home_pulse_end), style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat(lastSyncText, stringResource(R.string.home_hero_last_sync), Modifier.weight(1.4f), mono = false)
                HeroStat(sentLast24h?.toString() ?: "–", stringResource(R.string.home_hero_sent), Modifier.weight(1f))
                HeroStat(failedLast24h?.toString() ?: "–", stringResource(R.string.home_hero_failed), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            val pressSource = remember { MutableInteractionSource() }
            val pressed by pressSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(160, easing = EaseOutStrong), label = "press")
            Button(
                onClick = { showConfirmSheet = true },
                enabled = canSync && !isSyncing,
                interactionSource = pressSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.onPrimaryContainer,
                    contentColor = colors.primaryContainer,
                    disabledContainerColor = colors.onPrimaryContainer.copy(alpha = 0.3f),
                    disabledContentColor = colors.onPrimaryContainer.copy(alpha = 0.7f),
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale },
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.manual_sync_syncing))
                } else {
                    Icon(Icons.Filled.Sync, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.manual_sync_btn))
                }
            }
            AnimatedVisibility(visible = syncMessage != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Text(
                    syncMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** Strong ease-out: starts fast, so the UI feels like it answers at once. */
private val EaseOutStrong = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/**
 * Webhook deliveries per hour of the last 24 hours, like the activity bars of sms-webhook: a
 * solid part for the sent deliveries and a faded part on top for the failed ones. Empty hours
 * show as short stubs. The bars rise once from their stubs; during a sync the current hour
 * pulses.
 */
@Composable
private fun ActivityBars(events: List<Pair<Long, Boolean>>, now: Long, active: Boolean) {
    val ink = MaterialTheme.colorScheme.onPrimaryContainer
    val bars = remember(events, now / (60 * 60 * 1000L)) { hourlyBars(events, now) }
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(600, easing = EaseOutStrong)) }
    val pulse = rememberInfiniteTransition(label = "sync")
    val pulseAlpha by pulse.animateFloat(1f, 0.35f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val sent = events.count { it.second }
    val failed = events.size - sent
    val description = stringResource(R.string.home_pulse_description, sent, failed)
    val max = (bars.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics { contentDescription = description }
    ) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * 23) / 24
        val radius = CornerRadius(w / 2, w / 2)
        bars.forEachIndexed { index, bar ->
            // The time labels below flip in right-to-left languages, so the bars flip too.
            val start = index * (w + gap)
            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - w - start else start
            val alpha = if (active && index == bars.lastIndex) pulseAlpha else 1f
            if (bar.total == 0) {
                drawRoundRect(ink.copy(alpha = 0.12f), Offset(x, size.height - w), Size(w, w), radius)
                return@forEachIndexed
            }
            val full = (size.height * bar.total / max).coerceAtLeast(w)
            val height = w + (full - w) * rise.value
            drawRoundRect(ink.copy(alpha = alpha), Offset(x, size.height - height), Size(w, height), radius)
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier = Modifier, mono: Boolean = true) {
    Column(modifier) {
        Text(
            value,
            style = if (mono) MaterialTheme.typography.titleMedium.copy(fontFamily = Mono) else MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
    }
}
