package com.hcwebhook.app.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import com.hcwebhook.app.HealthDataType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    grantedPermissionsSet: Set<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalCount = HealthDataType.entries.size
    val grantedCount = HealthDataType.entries.count { type ->
        HealthPermission.getReadPermission(type.recordClass) in grantedPermissionsSet
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefragHandle() }
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
                        text = "Health Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$grantedCount of $totalCount permissions granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (grantedCount == totalCount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "Health Connect to Webhook requires access to your health and fitness data. This data is strictly used to fulfill the core functionality of the app: securely transmitting your chosen health metrics (such as steps, sleep, and heart rate) directly to your personal webhook URLs. We do not sell or share your data with any third parties. Only the data types you explicitly choose to configure below will be requested and synced.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(HealthDataType.entries) { dataType ->
                    val isGranted = HealthPermission.getReadPermission(dataType.recordClass) in grantedPermissionsSet
                    PermissionRow(
                        name = dataType.displayName,
                        rationale = dataType.rationale,
                        isGranted = isGranted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Open Health Connect settings button
            OutlinedButton(
                onClick = {
                    try {
                        // Try to open app-specific permissions page in Health Connect
                        val intent = Intent("androidx.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
                            putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                            setPackage("com.google.android.apps.healthdata")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            // Fallback: open Health Connect home (works on Samsung / Android embedded HC)
                            context.startActivity(Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"))
                        }
                    } catch (e: Exception) {
                        try {
                            context.startActivity(Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"))
                        } catch (ex: Exception) { /* ignore */ }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage in Health Connect")
            }
        }
    }
}

@Composable
private fun PermissionRow(name: String, rationale: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isGranted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Not granted",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun BottomSheetDefragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(width = 32.dp, height = 4.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ) {}
    }
}
