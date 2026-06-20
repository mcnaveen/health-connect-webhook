package com.hcwebhook.app.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.LocalHttpServerManager
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import com.hcwebhook.app.ServerRequestLog
import com.hcwebhook.app.WebhookConfig
import com.hcwebhook.app.WebhookLog
import com.hcwebhook.app.WebhookManager
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

private val LOG_LIMITS = listOf(25, 50, 100)
private val prettyJson = Json { prettyPrint = true }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    // ── Webhook logs ──────────────────────────────────────────────────────────
    var allLogs by remember { mutableStateOf(preferencesManager.getWebhookLogs()) }
    val urlToName = remember {
        preferencesManager.getWebhookConfigs()
            .filter { it.name.isNotBlank() }
            .associate { it.url to it.name }
    }
    var showClearSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var detailLog by remember { mutableStateOf<WebhookLog?>(null) }

    // filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableIntStateOf(0) } // 0=All 1=Success 2=Error
    var syncTypeFilter by remember { mutableIntStateOf(0) } // 0=All 1=Manual 2=Auto 3=Test 4=Notification
    var displayLimit by remember { mutableIntStateOf(50) }

    val uniqueUrls = remember(allLogs) { allLogs.map { it.url }.distinct().sorted() }

    val isFiltered = selectedUrl != null || statusFilter != 0 || syncTypeFilter != 0

    fun matchesFilter(log: WebhookLog): Boolean {
        val q = searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            val nameMatch = urlToName[log.url]?.lowercase()?.contains(q) == true
            val urlMatch = log.url.lowercase().contains(q)
            val errorMatch = log.errorMessage?.lowercase()?.contains(q) == true
            val typeMatch = log.dataType?.lowercase()?.contains(q) == true
            if (!nameMatch && !urlMatch && !errorMatch && !typeMatch) return false
        }
        return (selectedUrl == null || log.url == selectedUrl) &&
            when (statusFilter) { 1 -> log.success; 2 -> !log.success; else -> true } &&
            when (syncTypeFilter) { 1 -> log.syncType == "manual"; 2 -> log.syncType == "auto"; 3 -> log.syncType == "test"; 4 -> log.syncType == "notification"; else -> true }
    }

    val filtered = remember(allLogs, searchQuery, selectedUrl, statusFilter, syncTypeFilter, displayLimit) {
        allLogs.filter { matchesFilter(it) }.take(displayLimit)
    }

    // ── Clear Confirmation ────────────────────────────────────────────────────
    if (showClearSheet) {
        // Snapshot filter state at sheet-open time so the description stays stable
        val snapUrl = remember { selectedUrl }
        val snapStatus = remember { statusFilter }
        val snapSyncType = remember { syncTypeFilter }
        val snapIsFiltered = remember { snapUrl != null || snapStatus != 0 || snapSyncType != 0 }
        val snapLogsToRemove = remember {
            if (snapIsFiltered) {
                allLogs.filter { log ->
                    (snapUrl == null || log.url == snapUrl) &&
                    when (snapStatus) { 1 -> log.success; 2 -> !log.success; else -> true } &&
                    when (snapSyncType) { 1 -> log.syncType == "manual"; 2 -> log.syncType == "auto"; 3 -> log.syncType == "test"; 4 -> log.syncType == "notification"; else -> true }
                }
            } else allLogs
        }
        val clearDesc = buildString {
            if (!snapIsFiltered) {
                append("All ${allLogs.size} logs will be permanently deleted.")
            } else {
                append("${snapLogsToRemove.size} ")
                when (snapStatus) { 1 -> append("successful "); 2 -> append("failed ") }
                append(if (snapLogsToRemove.size == 1) "log" else "logs")
                snapUrl?.let { append(" from ${it.removePrefix("https://").removePrefix("http://").substringBefore("/")}") }
                when (snapSyncType) { 1 -> append(" (manual)"); 2 -> append(" (auto)"); 3 -> append(" (test)"); 4 -> append(" (notification)") }
                append(" will be permanently deleted.")
            }
        }
        ModalBottomSheet(onDismissRequest = { showClearSheet = false }) {
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
                Text(
                    if (snapIsFiltered) "Clear filtered logs?" else stringResource(R.string.logs_clear_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    clearDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (snapIsFiltered) {
                            val ids = snapLogsToRemove.map { it.id }.toSet()
                            preferencesManager.removeWebhookLogs(ids)
                            allLogs = allLogs.filter { it.id !in ids }
                        } else {
                            preferencesManager.clearWebhookLogs()
                            allLogs = emptyList()
                        }
                        showClearSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (snapIsFiltered) "Clear ${snapLogsToRemove.size} logs" else stringResource(R.string.action_clear_logs))
                }
                OutlinedButton(
                    onClick = { showClearSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    // ── Filter Sheet ──────────────────────────────────────────────────────────
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Filter Logs", style = MaterialTheme.typography.titleMedium)

                // Webhook URL
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Webhook URL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            FilterUrlRow(
                                label = "All URLs",
                                selected = selectedUrl == null,
                                onClick = { selectedUrl = null }
                            )
                            uniqueUrls.forEachIndexed { i, url ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                FilterUrlRow(
                                    label = url,
                                    selected = selectedUrl == url,
                                    onClick = { selectedUrl = url }
                                )
                            }
                        }
                    }
                }

                // Status
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to stringResource(R.string.logs_filter_all),
                            1 to stringResource(R.string.logs_filter_success),
                            2 to stringResource(R.string.logs_filter_error)
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = statusFilter == idx,
                                onClick = { statusFilter = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Sync Type
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sync Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to stringResource(R.string.logs_filter_all),
                            1 to stringResource(R.string.logs_sync_manual),
                            2 to stringResource(R.string.logs_sync_auto),
                            3 to stringResource(R.string.logs_sync_test),
                            4 to "Notification"
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = syncTypeFilter == idx,
                                onClick = { syncTypeFilter = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Limit
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Show",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LOG_LIMITS.forEach { limit ->
                            FilterChip(
                                selected = displayLimit == limit,
                                onClick = { displayLimit = limit },
                                label = { Text("$limit", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Retention
                var maxLogs by remember { mutableIntStateOf(preferencesManager.getMaxLogs()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Keep (max stored)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreferencesManager.MAX_LOG_OPTIONS.forEach { limit ->
                            FilterChip(
                                selected = maxLogs == limit,
                                onClick = {
                                    maxLogs = limit
                                    preferencesManager.setMaxLogs(limit)
                                },
                                label = { Text("$limit", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Detail Sheet ──────────────────────────────────────────────────────────
    detailLog?.let { log ->
        ModalBottomSheet(
            onDismissRequest = { detailLog = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LogDetailSheet(
                log = log,
                preferencesManager = preferencesManager,
                onDelete = {
                    preferencesManager.removeWebhookLog(log.id)
                    allLogs = allLogs.filter { it.id != log.id }
                    detailLog = null
                }
            )
        }
    }

    // ── Server logs ───────────────────────────────────────────────────────────
    var serverLogs by remember { mutableStateOf(LocalHttpServerManager.getRequestLogs()) }
    var showClearServerSheet by remember { mutableStateOf(false) }
    var showServerFilterSheet by remember { mutableStateOf(false) }
    var detailServerLog by remember { mutableStateOf<ServerRequestLog?>(null) }

    // server log filter state
    var serverMethodFilter by remember { mutableIntStateOf(0) }  // 0=All 1=GET 2=POST
    var serverStatusFilter by remember { mutableIntStateOf(0) }  // 0=All 1=2xx 2=4xx/5xx
    var serverPathFilter by remember { mutableStateOf<String?>(null) }
    var serverDisplayLimit by remember { mutableIntStateOf(50) }

    val serverIsFiltered = serverMethodFilter != 0 || serverStatusFilter != 0 || serverPathFilter != null
    val uniqueServerPaths = remember(serverLogs) { serverLogs.map { it.path }.distinct().sorted() }

    fun matchesServerFilter(log: ServerRequestLog) =
        (when (serverMethodFilter) { 1 -> log.method == "GET"; 2 -> log.method == "POST"; else -> true }) &&
        (when (serverStatusFilter) { 1 -> log.statusCode in 200..299; 2 -> log.statusCode >= 400; else -> true }) &&
        (serverPathFilter == null || log.path == serverPathFilter)

    val filteredServerLogs = remember(serverLogs, serverMethodFilter, serverStatusFilter, serverPathFilter, serverDisplayLimit) {
        serverLogs.filter { matchesServerFilter(it) }.take(serverDisplayLimit)
    }

    if (showServerFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showServerFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Filter Server Logs", style = MaterialTheme.typography.titleMedium)

                // Method
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Method", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "All", 1 to "GET", 2 to "POST").forEach { (idx, label) ->
                            FilterChip(
                                selected = serverMethodFilter == idx,
                                onClick = { serverMethodFilter = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Status
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "All", 1 to "2xx", 2 to "4xx / 5xx").forEach { (idx, label) ->
                            FilterChip(
                                selected = serverStatusFilter == idx,
                                onClick = { serverStatusFilter = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Path
                if (uniqueServerPaths.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Path", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                FilterUrlRow(label = "All paths", selected = serverPathFilter == null, onClick = { serverPathFilter = null })
                                uniqueServerPaths.forEachIndexed { i, path ->
                                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    FilterUrlRow(label = path, selected = serverPathFilter == path, onClick = { serverPathFilter = path })
                                }
                            }
                        }
                    }
                }

                // Limit
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Show", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LOG_LIMITS.forEach { limit ->
                            FilterChip(
                                selected = serverDisplayLimit == limit,
                                onClick = { serverDisplayLimit = limit },
                                label = { Text("$limit", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }

    detailServerLog?.let { log ->
        ModalBottomSheet(
            onDismissRequest = { detailServerLog = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ServerLogDetailSheet(log = log)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        while (pagerState.currentPage == 1) {
            serverLogs = LocalHttpServerManager.getRequestLogs()
            delay(2_000)
        }
    }

    if (showClearServerSheet) {
        ModalBottomSheet(onDismissRequest = { showClearServerSheet = false }) {
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
                Text("Clear server logs?", style = MaterialTheme.typography.titleLarge)
                Text(
                    "All ${serverLogs.size} in-memory server request logs will be cleared.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        LocalHttpServerManager.clearRequestLogs()
                        serverLogs = emptyList()
                        showClearServerSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_clear_logs))
                }
                OutlinedButton(
                    onClick = { showClearServerSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }

    // ── Main UI ───────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Webhooks") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Server") }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
        if (page == 0) { Column(modifier = Modifier.fillMaxSize()) {
            // ── Webhook logs tab ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.logs_title), style = MaterialTheme.typography.titleLarge)
                    if (allLogs.isNotEmpty()) {
                        val totalMatching = remember(allLogs, selectedUrl, statusFilter, syncTypeFilter) {
                            allLogs.count { matchesFilter(it) }
                        }
                        Text(
                            if (isFiltered) "$totalMatching of ${allLogs.size} logs"
                            else stringResource(R.string.logs_count, allLogs.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (allLogs.isNotEmpty()) {
                        IconButton(onClick = {
                            val json = prettyJson.encodeToString(allLogs)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.logs_export_intent_title))
                            }
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.logs_action_export)))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.logs_action_export))
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                        }
                        TextButton(onClick = { showClearSheet = true }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }

            if (allLogs.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search URL, error, data type…", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        } }
                    } else null
                )
            }

            if (allLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.logs_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No logs match.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(filtered, key = { it.id }) { log ->
                        LogRow(log = log, webhookName = urlToName[log.url], onClick = { detailLog = log })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        } } else { Column(modifier = Modifier.fillMaxSize()) {
            // ── Server logs tab ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Server Logs", style = MaterialTheme.typography.titleLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            if (serverLogs.isEmpty()) "Live · no requests yet"
                            else if (serverIsFiltered) "Live · ${filteredServerLogs.size} of ${serverLogs.size} requests"
                            else "Live · ${serverLogs.size} requests",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (serverLogs.isNotEmpty()) {
                        IconButton(onClick = { showServerFilterSheet = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Filter",
                                tint = if (serverIsFiltered) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showClearServerSheet = true }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }

            if (serverLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No requests yet. Start the local HTTP server and make a request.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else if (filteredServerLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No logs match the current filter.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(filteredServerLogs, key = { it.id }) { log ->
                        ServerLogRow(log = log, onClick = { detailServerLog = log })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        } } // end server tab Column + HorizontalPager
        } // end pager lambda
    }
}

@Composable
private fun ServerLogRow(log: ServerRequestLog, onClick: () -> Unit) {
    val methodColor = when (log.method) {
        "GET"  -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        else   -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusColor = when {
        log.statusCode in 200..299 -> Color(0xFF4CAF50)
        log.statusCode >= 400      -> MaterialTheme.colorScheme.error
        else                       -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            log.method,
            style = MaterialTheme.typography.labelSmall,
            color = methodColor,
            modifier = Modifier.width(36.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                log.path,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${log.clientIp} · ${formatTimestamp(log.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${log.statusCode}",
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
            Text(
                "${log.responseTimeMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ServerLogDetailSheet(log: ServerRequestLog) {
    val clipboardManager = LocalClipboardManager.current
    val methodColor = when (log.method) {
        "GET"  -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        else   -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusColor = when {
        log.statusCode in 200..299 -> Color(0xFF4CAF50)
        log.statusCode >= 400      -> MaterialTheme.colorScheme.error
        else                       -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    log.method,
                    style = MaterialTheme.typography.titleMedium,
                    color = methodColor
                )
                Text(
                    "${log.statusCode}",
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor
                )
            }
            IconButton(onClick = {
                val text = buildString {
                    appendLine("Method: ${log.method}")
                    appendLine("Path: ${log.path}")
                    appendLine("Status: ${log.statusCode}")
                    appendLine("Response Time: ${log.responseTimeMs}ms")
                    appendLine("Client IP: ${log.clientIp}")
                    appendLine("Timestamp: ${formatFullTimestamp(log.timestamp)}")
                }
                clipboardManager.setText(AnnotatedString(text))
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DetailField("Path", log.path)
        DetailField("Timestamp", formatFullTimestamp(log.timestamp))
        DetailField("Client IP", log.clientIp)
        DetailField("Response Time", "${log.responseTimeMs}ms")
        DetailField("Status Code", "${log.statusCode}", valueColor = statusColor)
    }
}

@Composable
private fun FilterUrlRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun LogRow(log: WebhookLog, webhookName: String? = null, onClick: () -> Unit) {
    val dotColor = if (log.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            if (webhookName != null) {
                Text(
                    text = webhookName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = log.url,
                style = if (webhookName != null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (webhookName != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                log.syncType?.let { type ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            when (type) {
                                "manual" -> stringResource(R.string.logs_sync_manual)
                                "test" -> stringResource(R.string.logs_sync_test)
                                "notification" -> "Notification"
                                else -> stringResource(R.string.logs_sync_auto)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = "${log.statusCode ?: stringResource(R.string.logs_status_err)}",
            style = MaterialTheme.typography.labelMedium,
            color = if (log.success)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LogDetailSheet(
    log: WebhookLog,
    preferencesManager: PreferencesManager,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val successColor = Color(0xFF4CAF50)
    val statusColor = if (log.success) successColor else MaterialTheme.colorScheme.error
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var retryLoading by remember { mutableStateOf(false) }
    var retryResult by remember { mutableStateOf<Boolean?>(null) }
    var retryMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val prettyPayload = remember(log.payload) {
        log.payload?.let {
            try { prettyJson.encodeToString(Json.parseToJsonElement(it)) } catch (_: Exception) { it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    if (log.success) "Success" else "Failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!log.success && log.payload != null) {
                    IconButton(
                        onClick = {
                            retryLoading = true
                            retryResult = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    val configs = preferencesManager.getWebhookConfigs()
                                    val config = configs.find { it.url == log.url }
                                        ?: WebhookConfig(url = log.url)
                                    WebhookManager(
                                        webhookConfigs = listOf(config.copy(isEnabled = true)),
                                        context = context,
                                        dataType = log.dataType,
                                        recordCount = log.recordCount,
                                        syncType = "manual",
                                        payload = log.payload
                                    ).postData(log.payload)
                                }
                                retryLoading = false
                                retryResult = result.isSuccess
                                retryMessage = result.exceptionOrNull()?.message ?: ""
                            }
                        },
                        enabled = !retryLoading
                    ) {
                        if (retryLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.webhooks_test_retry),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(prettyJson.encodeToString(log)))
                }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy as JSON",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        retryResult?.let { success ->
            Text(
                text = if (success) stringResource(R.string.webhooks_retry_success)
                       else stringResource(R.string.webhooks_retry_failed, retryMessage),
                style = MaterialTheme.typography.labelSmall,
                color = if (success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }

        if (showDeleteConfirm) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Delete this log?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onDelete) {
                            Text(
                                stringResource(R.string.action_delete),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Fields
        DetailField("URL", log.url)
        DetailField("Timestamp", formatFullTimestamp(log.timestamp))
        DetailField("Status Code", "${log.statusCode ?: "—"}")
        log.responseTimeMs?.let { DetailField("Response Time", "${it} ms") }
        log.syncType?.let {
            DetailField(
                "Sync Type",
                when (it) {
                    "manual" -> stringResource(R.string.logs_sync_manual)
                    "test" -> stringResource(R.string.logs_sync_test)
                    "notification" -> "Notification"
                    else -> stringResource(R.string.logs_sync_auto)
                }
            )
        }
        if (log.recordCount != null) DetailField("Records", "${log.recordCount}")
        if (!log.success && log.errorMessage != null) {
            DetailField("Error", log.errorMessage, valueColor = MaterialTheme.colorScheme.error)
        }

        // Payload
        if (prettyPayload != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Payload",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prettyPayload,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFullTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy · HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
