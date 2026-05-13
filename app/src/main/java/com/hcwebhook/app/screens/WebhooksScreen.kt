package com.hcwebhook.app.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.R
import com.hcwebhook.app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WebhooksScreen(onOpenNotificationsSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val globalEnabledTypes = remember { preferencesManager.getEnabledDataTypes().map { it.name }.toSet() }
    val globalNotificationConfigs = remember { preferencesManager.getNotificationConfigs() }
    val appVersion = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown" }
        catch (_: Exception) { "unknown" }
    }

    var webhookConfigs by remember { mutableStateOf(preferencesManager.getWebhookConfigs()) }
    var newUrl by remember { mutableStateOf("") }
    var sheetIndex by remember { mutableStateOf(-1) }
    val showSheet = sheetIndex >= 0 && sheetIndex in webhookConfigs.indices

    LaunchedEffect(webhookConfigs) {
        preferencesManager.setWebhookConfigs(webhookConfigs)
    }

    // ── Edit Bottom Sheet ─────────────────────────────────────────────────────
    if (showSheet) {
        val config = webhookConfigs[sheetIndex]
        val capturedIndex = sheetIndex

        var editUrl by remember(capturedIndex) { mutableStateOf(config.url) }
        var currentHeaders by remember(capturedIndex) { mutableStateOf(config.headers) }
        var manageHeaders by remember(capturedIndex) { mutableStateOf(config.headers.isNotEmpty()) }
        var newKey by remember(capturedIndex) { mutableStateOf("") }
        var newValue by remember(capturedIndex) { mutableStateOf("") }
        var jsonPaste by remember(capturedIndex) { mutableStateOf("") }
        var headerTab by remember(capturedIndex) { mutableIntStateOf(0) } // 0=Form, 1=JSON
        var filterAll by remember(capturedIndex) { mutableStateOf(config.dataTypeFilter == null) }
        var selectedTypes by remember(capturedIndex) {
            mutableStateOf(config.dataTypeFilter ?: globalEnabledTypes)
        }
        var selectedNotificationIds by remember(capturedIndex) { mutableStateOf(config.notificationConfigIds) }
        var showDeleteConfirm by remember(capturedIndex) { mutableStateOf(false) }
        var testLoading by remember(capturedIndex) { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val hasUnsavedChanges by remember(editUrl, currentHeaders, filterAll, selectedTypes, selectedNotificationIds) {
            derivedStateOf {
                editUrl.trim() != config.url ||
                currentHeaders != config.headers ||
                filterAll != (config.dataTypeFilter == null) ||
                (!filterAll && selectedTypes != (config.dataTypeFilter ?: globalEnabledTypes)) ||
                selectedNotificationIds != config.notificationConfigIds
            }
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { sheetIndex = -1 },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.webhooks_edit_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasUnsavedChanges) {
                            Text(
                                stringResource(R.string.webhooks_unsaved_changes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                if (config.isEnabled) stringResource(R.string.webhooks_status_active)
                                else stringResource(R.string.webhooks_status_disabled),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (config.isEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { enabled ->
                            val list = webhookConfigs.toMutableList()
                            list[capturedIndex] = config.copy(isEnabled = enabled)
                            webhookConfigs = list
                        }
                    )
                }

                // ── URL edit ─────────────────────────────────────────────────
                OutlinedTextField(
                    value = editUrl,
                    onValueChange = { editUrl = it },
                    label = { Text(stringResource(R.string.webhooks_new_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    isError = editUrl.isNotBlank() && !editUrl.startsWith("http://") && !editUrl.startsWith("https://")
                )

                // ── Data Types ───────────────────────────────────────────────
                SheetSection {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.webhooks_data_types_section),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.webhooks_data_types_send_all),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = filterAll,
                                onCheckedChange = { all ->
                                    filterAll = all
                                    if (all) selectedTypes = globalEnabledTypes
                                }
                            )
                        }

                        if (!filterAll) {
                            if (globalEnabledTypes.isEmpty()) {
                                Text(
                                    stringResource(R.string.webhooks_data_types_none_global),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    globalEnabledTypes.sorted().forEach { typeName ->
                                        val label = typeName.replace('_', ' ').lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                        FilterChip(
                                            selected = typeName in selectedTypes,
                                            onClick = {
                                                selectedTypes = if (typeName in selectedTypes)
                                                    selectedTypes - typeName
                                                else
                                                    selectedTypes + typeName
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            leadingIcon = if (typeName in selectedTypes) {
                                                { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Headers ───────────────────────────────────────────────────
                SheetSection {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.webhooks_headers_manage_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Switch(
                                checked = manageHeaders,
                                onCheckedChange = { enabled -> 
                                    manageHeaders = enabled
                                    if (!enabled) currentHeaders = emptyMap()
                                }
                            )
                        }

                        if (manageHeaders) {
                            if (currentHeaders.isEmpty()) {
                                Text(
                                    stringResource(R.string.webhooks_headers_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                currentHeaders.forEach { (key, value) ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                Text(value, style = MaterialTheme.typography.bodySmall)
                                            }
                                            IconButton(
                                                onClick = { currentHeaders = currentHeaders - key },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = stringResource(R.string.webhooks_headers_action_remove),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            TabRow(
                                selectedTabIndex = headerTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                            ) {
                                Tab(selected = headerTab == 0, onClick = { headerTab = 0 }) {
                                    Text(
                                        stringResource(R.string.webhooks_headers_tab_form),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                                Tab(selected = headerTab == 1, onClick = { headerTab = 1 }) {
                                    Text(
                                        stringResource(R.string.webhooks_headers_tab_json),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }

                            if (headerTab == 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newKey,
                                        onValueChange = { newKey = it },
                                        label = { Text(stringResource(R.string.webhooks_headers_key_label)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedTextField(
                                        value = newValue,
                                        onValueChange = { newValue = it },
                                        label = { Text(stringResource(R.string.webhooks_headers_value_label)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
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
                            } else {
                                OutlinedTextField(
                                    value = jsonPaste,
                                    onValueChange = { jsonPaste = it },
                                    label = { Text(stringResource(R.string.webhooks_headers_json_label)) },
                                    placeholder = { Text("{\"Authorization\": \"Bearer token\"}", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val obj = JSONObject(jsonPaste.trim())
                                            val parsed = mutableMapOf<String, String>()
                                            obj.keys().forEach { key -> parsed[key] = obj.getString(key) }
                                            currentHeaders = currentHeaders + parsed
                                            jsonPaste = ""
                                            headerTab = 0
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.webhooks_headers_json_error), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.webhooks_headers_json_apply))
                                }
                            }
                        }
                    }
                }

                SheetSection {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.webhooks_notification_section),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            val selectedCount = selectedNotificationIds.size
                            val selectedName = if (selectedCount == 0) {
                                stringResource(R.string.webhooks_notification_none)
                            } else if (selectedCount == 1) {
                                globalNotificationConfigs.find { it.id == selectedNotificationIds.first() }?.let {
                                    "${it.providerType.displayName} (${it.displayIdentifier})"
                                } ?: "$selectedCount selected"
                            } else {
                                "$selectedCount providers selected"
                            }

                            OutlinedTextField(
                                value = selectedName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add New Provider...") },
                                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) },
                                    onClick = {
                                        expanded = false
                                        onOpenNotificationsSettings()
                                    }
                                )
                                HorizontalDivider()
                                
                                if (globalNotificationConfigs.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No providers configured", fontStyle = FontStyle.Italic) },
                                        onClick = { expanded = false }
                                    )
                                } else {
                                    globalNotificationConfigs.forEach { notif ->
                                        DropdownMenuItem(
                                            text = { Text("${notif.providerType.displayName} (${notif.displayIdentifier})") },
                                            leadingIcon = {
                                                Checkbox(
                                                    checked = notif.id in selectedNotificationIds,
                                                    onCheckedChange = null
                                                )
                                            },
                                            onClick = {
                                                selectedNotificationIds = if (notif.id in selectedNotificationIds) {
                                                    selectedNotificationIds - notif.id
                                                } else {
                                                    selectedNotificationIds + notif.id
                                                }
                                                // Don't close the dropdown on click to allow multiple selections
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Actions ──────────────────────────────────────────────────
                val urlValid = editUrl.trim().let { it.startsWith("http://") || it.startsWith("https://") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            testLoading = true
                            val testConfig = WebhookConfig(
                                url = editUrl.trim(),
                                headers = currentHeaders,
                                isEnabled = true
                            )
                            val typesForMock = if (filterAll) globalEnabledTypes else selectedTypes
                            val mockPayload = MockPayloadBuilder.build(typesForMock.ifEmpty { null }, appVersion)
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    WebhookManager(
                                        webhookConfigs = listOf(testConfig),
                                        context = context,
                                        syncType = "test",
                                        payload = mockPayload
                                    ).postData(mockPayload)
                                }
                                val log = withContext(Dispatchers.IO) {
                                    preferencesManager.getWebhookLogs()
                                        .firstOrNull { it.syncType == "test" && it.url == testConfig.url }
                                }
                                testLoading = false
                                val detail = buildString {
                                    val code = log?.statusCode
                                    val ms = log?.responseTimeMs
                                    if (code != null) append("$code")
                                    if (ms != null) { if (code != null) append(" · "); append("${ms}ms") }
                                    if (isEmpty()) append(result.exceptionOrNull()?.message ?: "Failed")
                                }
                                if (result.isSuccess) {
                                    Toast.makeText(context, context.getString(R.string.webhooks_test_success, detail), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.webhooks_test_failed, detail), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !testLoading && urlValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (testLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.webhooks_action_test))
                    }

                    Button(
                        onClick = {
                            val trimmedUrl = editUrl.trim()
                            if (trimmedUrl.isEmpty() || (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))) {
                                Toast.makeText(context, context.getString(R.string.webhooks_toast_invalid_url), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val newFilter = if (filterAll) null else selectedTypes.ifEmpty { null }
                            val list = webhookConfigs.toMutableList()
                            list[capturedIndex] = webhookConfigs[capturedIndex].copy(
                                url = trimmedUrl,
                                headers = currentHeaders,
                                dataTypeFilter = newFilter,
                                notificationConfigIds = selectedNotificationIds
                            )
                            webhookConfigs = list
                            sheetIndex = -1
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }

                // ── Delete ────────────────────────────────────────────────────
                if (showDeleteConfirm) {
                    Button(
                        onClick = {
                            webhookConfigs = webhookConfigs.toMutableList().apply { removeAt(capturedIndex) }
                            sheetIndex = -1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.webhooks_delete_title))
                    }
                } else {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }

    // ── Main List ─────────────────────────────────────────────────────────────
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.webhooks_section_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (webhookConfigs.isEmpty()) {
                        Text(
                            stringResource(R.string.webhooks_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    webhookConfigs.forEachIndexed { index, config ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = config.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (config.isEnabled)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    val meta = buildList {
                                        val h = config.getHeaderCount()
                                        val f = config.dataTypeFilter?.size
                                        if (h > 0) add(stringResource(R.string.webhooks_headers_count, h))
                                        if (f != null) add(stringResource(R.string.webhooks_data_types_filter_count, f))
                                    }.joinToString(" · ")
                                    if (meta.isNotEmpty()) {
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (config.isEnabled) 1f else 0.4f
                                            )
                                        )
                                    }
                                }

                                Box(modifier = Modifier.graphicsLayer(scaleX = 0.75f, scaleY = 0.75f)) {
                                    Switch(
                                        checked = config.isEnabled,
                                        onCheckedChange = { enabled ->
                                            val updated = webhookConfigs.toMutableList()
                                            updated[index] = config.copy(isEnabled = enabled)
                                            webhookConfigs = updated
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = { sheetIndex = index },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.webhooks_action_edit_headers),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
}

@Composable
private fun SheetSection(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
