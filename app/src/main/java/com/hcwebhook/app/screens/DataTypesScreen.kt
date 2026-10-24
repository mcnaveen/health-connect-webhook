package com.hcwebhook.app.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import com.hcwebhook.app.HealthDataType
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import com.hcwebhook.app.category
import com.hcwebhook.app.DataCategory
import com.hcwebhook.app.defaultResolutionMinutes
import com.hcwebhook.app.resolutionFamily
import com.hcwebhook.app.resolutionSubtitleResId
import com.hcwebhook.app.supportsBucketDedup
import com.hcwebhook.app.ui.AppSwitch
import com.hcwebhook.app.ui.GroupItem
import com.hcwebhook.app.ui.SectionTitle
import com.hcwebhook.app.ui.SettingRow
import com.hcwebhook.app.ui.SupportingText

/**
 * Health data: which types the app sends, their access in Health Connect and their resolution.
 * One full screen, grouped by the Health Connect categories, instead of a long list in a sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTypesScreen(
    grantedPermissionsSet: Set<String>,
    permissionLauncher: ActivityResultLauncher<Set<String>>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var enabledDataTypes by remember { mutableStateOf(preferencesManager.getEnabledDataTypes()) }
    var resolutions by remember { mutableStateOf(preferencesManager.getDataTypeResolutions()) }
    var bucketDedup by remember { mutableStateOf(preferencesManager.isBucketDedup()) }
    var expanded by remember { mutableStateOf<HealthDataType?>(null) }

    fun hasAccess(type: HealthDataType) = HealthPermission.getReadPermission(type.recordClass) in grantedPermissionsSet
    val missingPermissions = remember(enabledDataTypes, grantedPermissionsSet) {
        val missing = enabledDataTypes.filterNot(::hasAccess)
            .map { HealthPermission.getReadPermission(it.recordClass) }
            .toMutableSet()
        if (missing.isNotEmpty() && HISTORY_PERMISSION !in grantedPermissionsSet) missing += HISTORY_PERMISSION
        missing.toSet()
    }
    val missingTypeCount = enabledDataTypes.count { !hasAccess(it) }

    fun toggle(type: HealthDataType, on: Boolean) {
        enabledDataTypes = if (on) enabledDataTypes + type else enabledDataTypes - type
        preferencesManager.setEnabledDataTypes(enabledDataTypes)
        if (!on && expanded == type) expanded = null
    }

    // MainActivity already pads the system bars, so this screen adds no insets of its own.
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Column {
                        Text(stringResource(R.string.health_data_title))
                        Text(
                            stringResource(R.string.health_data_count, enabledDataTypes.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = missingTypeCount > 0, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Button(
                        onClick = {
                            try {
                                permissionLauncher.launch(missingPermissions)
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.config_toast_error, e.message), Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                    ) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.health_data_allow, missingTypeCount))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item {
                SupportingText(stringResource(R.string.health_data_intro), Modifier.padding(bottom = 8.dp))
            }
            DataCategory.entries.forEach { category ->
                val types = HealthDataType.entries.filter { it.category == category }
                if (types.isEmpty()) return@forEach
                item(key = "category_${category.name}") { SectionTitle(stringResource(category.titleResId), Modifier.padding(top = 12.dp)) }
                itemsIndexed(types, key = { _, type -> "type_${type.name}" }) { index, type ->
                    val enabled = type in enabledDataTypes
                    val family = type.resolutionFamily
                    val selected = resolutions[type] ?: type.defaultResolutionMinutes
                    val options = if (family != null) resolutionChipOptions(family) else emptyList()
                    val summary = when {
                        enabled && !hasAccess(type) -> stringResource(R.string.health_data_no_access)
                        enabled && family != null -> options.firstOrNull { it.first == selected }?.second
                        else -> null
                    }
                    GroupItem(index, types.size) {
                        Column {
                            ListItem(
                                headlineContent = { Text(stringResource(type.nameResId), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = summary?.let {
                                    {
                                        Text(
                                            it,
                                            color = if (enabled && !hasAccess(type)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                leadingContent = { Icon(iconForDataType(type), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingContent = { AppSwitch(enabled) { toggle(type, it) } },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    // A row with settings opens them; other rows switch the type on or off.
                                    if (enabled && family != null) expanded = if (expanded == type) null else type
                                    else toggle(type, !enabled)
                                },
                            )
                            AnimatedVisibility(
                                visible = enabled && family != null && expanded == type,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                if (family != null) {
                                    ResolutionChipRow(
                                        subtitle = stringResource(resolutionSubtitleResId(family)),
                                        options = options,
                                        selected = selected,
                                        onSelect = {
                                            resolutions = resolutions + (type to it)
                                            preferencesManager.setDataTypeResolution(type, it)
                                        },
                                        bucketDedup = bucketDedup.takeIf { type.supportsBucketDedup },
                                        onBucketDedupChange = {
                                            bucketDedup = it
                                            preferencesManager.setBucketDedup(it)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (index < types.lastIndex) Spacer(Modifier.height(2.dp))
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                GroupItem(0, 1) {
                    SettingRow(
                        title = stringResource(R.string.config_manage_hc),
                        trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { openHealthConnectPermissions(context) },
                    )
                }
                Spacer(Modifier.height(16.dp))
                SupportingText(stringResource(R.string.dt_hc_rationale))
            }
        }
    }
}

private const val HISTORY_PERMISSION = "android.permission.health.READ_HEALTH_DATA_HISTORY"

/** This app's page in Health Connect, or the Health Connect home (Samsung, embedded Health Connect). */
private fun openHealthConnectPermissions(context: android.content.Context) {
    val appPage = Intent("androidx.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
    }
    val opened = listOf(appPage, Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")).any { intent ->
        try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
    if (!opened) Toast.makeText(context, R.string.err_hc_not_available, Toast.LENGTH_SHORT).show()
}
