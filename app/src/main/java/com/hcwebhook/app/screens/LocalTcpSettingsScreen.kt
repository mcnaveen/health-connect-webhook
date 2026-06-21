package com.hcwebhook.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hcwebhook.app.LocalHttpServerService
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
private fun CodeBlock(label: String, code: String, onCopy: () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun getLocalIpAddress(context: android.content.Context): String {
    // WifiManager gives the most reliable Wi-Fi IP on all API levels
    try {
        val wm = context.applicationContext
            .getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val ip = wm?.connectionInfo?.ipAddress
        if (ip != null && ip != 0) {
            return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
        }
    } catch (_: Exception) {}
    // Fallback: scan all interfaces (covers Ethernet, Tailscale, etc.)
    return try {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses?.toList() ?: emptyList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "0.0.0.0"
    } catch (_: Exception) {
        "0.0.0.0"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalHttpSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val preferencesManager = remember { PreferencesManager(context) }
    var localTcpEnabled by remember { mutableStateOf(preferencesManager.isLocalTcpEnabled()) }
    var localTcpPort by remember { mutableStateOf(preferencesManager.getLocalTcpPort().toString()) }
    var savedPort by remember { mutableStateOf(preferencesManager.getLocalTcpPort()) }
    var authEnabled by remember { mutableStateOf(preferencesManager.isLocalHttpAuthEnabled()) }
    var authToken by remember { mutableStateOf(preferencesManager.getLocalHttpToken()) }
    val writeToken = remember { preferencesManager.getWriteToken() }
    val deviceIp = remember { getLocalIpAddress(context) }
    BackHandler(onBack = onBack)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        LocalHttpServerService.start(context)
    }

    fun startServerWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            LocalHttpServerService.start(context)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.about_local_http_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.config_local_tcp_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.config_local_tcp_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(7.dp),
                                    shape = CircleShape,
                                    color = if (localTcpEnabled) Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ) {}
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (localTcpEnabled) "Running on port $savedPort" else "Stopped",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (localTcpEnabled) Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = localTcpEnabled,
                            onCheckedChange = { enabled ->
                                localTcpEnabled = enabled
                                preferencesManager.setLocalTcpEnabled(enabled)
                                if (enabled) {
                                    val port = localTcpPort.toIntOrNull()
                                    if (port != null && port in 1024..65535) {
                                        preferencesManager.setLocalTcpPort(port)
                                        savedPort = port
                                    }
                                    startServerWithPermission()
                                } else {
                                    LocalHttpServerService.stop(context)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = localTcpPort,
                            onValueChange = { localTcpPort = it.filter { ch -> ch.isDigit() }.take(5) },
                            label = { Text(stringResource(R.string.config_local_tcp_port_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val port = localTcpPort.toIntOrNull()
                                if (port == null || port !in 1024..65535) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.config_local_tcp_invalid_port),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                preferencesManager.setLocalTcpPort(port)
                                localTcpPort = port.toString()
                                savedPort = port
                                if (localTcpEnabled) {
                                    LocalHttpServerService.stop(context)
                                    LocalHttpServerService.start(context)
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.config_local_tcp_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }

            if (localTcpEnabled) {
                data class Endpoint(val method: String, val path: String, val description: String)
                val endpoints = listOf(
                    Endpoint("GET",  "/",            "Health data · ?days=N"),
                    Endpoint("GET",  "/ping",         "Liveness check"),
                    Endpoint("GET",  "/latest",       "Last synced payload"),
                    Endpoint("GET",  "/logs",         "Webhook logs · ?limit · ?success · ?dataType · ?since"),
                    Endpoint("GET",  "/stats",        "Aggregate stats — totals, success rate, avg response time"),
                    Endpoint("GET",  "/health",       "Server uptime & last sync info"),
                    Endpoint("GET",  "/server-logs",  "HTTP access log · ?limit · ?method · ?path · ?since"),
                    Endpoint("POST", "/sync",         "Trigger sync · ?days=N"),
                    Endpoint("POST", "/write",        "Write a record to Health Connect · header X-Write-Token"),
                )
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Endpoints", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Bound to 0.0.0.0 — accessible via LAN, Tailscale, or any interface",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        endpoints.forEach { endpoint ->
                            val baseUrl = "http://$deviceIp:$savedPort"
                            val authFlag = if (authEnabled) " -H \"Authorization: Bearer $authToken\"" else ""
                            val copyText = when {
                                endpoint.path == "/write" ->
                                    "curl -X POST$authFlag -H \"X-Write-Token: $writeToken\" -H \"Content-Type: application/json\" " +
                                        "-d '{\"type\":\"weight\",\"data\":{\"kilograms\":75.0,\"time\":\"2024-01-01T08:00:00Z\"}}' $baseUrl/write"
                                endpoint.method == "POST" -> "curl -X POST$authFlag $baseUrl${endpoint.path}"
                                else -> "$baseUrl${endpoint.path}"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = endpoint.method,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (endpoint.method == "POST") Color(0xFF2196F3) else Color(0xFF4CAF50),
                                    modifier = Modifier.width(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = endpoint.path,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = endpoint.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(copyText))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Require auth token", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = authEnabled,
                                onCheckedChange = { enabled ->
                                    authEnabled = enabled
                                    preferencesManager.setLocalHttpAuthEnabled(enabled)
                                }
                            )
                        }
                        if (authEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Pass as header: Authorization: Bearer <token>",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = authToken,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(authToken))
                                        Toast.makeText(context, "Token copied", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = "Copy token",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    authToken = preferencesManager.regenerateLocalHttpToken()
                                    Toast.makeText(context, "Token regenerated", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Regenerate token", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Write token", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Required on POST /write. Pass as header: X-Write-Token: <token>",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = writeToken,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(writeToken))
                                    Toast.makeText(context, "Write token copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy write token",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val baseUrl = "http://$deviceIp:$savedPort"
                val authHeader = if (authEnabled) " \\\n  -H \"Authorization: Bearer $authToken\"" else ""
                val curlExample = "curl$authHeader \\\n  $baseUrl/"
                val fetchHeaders = if (authEnabled) "\n  headers: { Authorization: 'Bearer $authToken' }" else ""
                val fetchExample = "const res = await fetch('$baseUrl/', {$fetchHeaders\n})\nconst data = await res.json()"
                val pythonHeaders = if (authEnabled) "\nheaders = {'Authorization': 'Bearer $authToken'}\nr = requests.get('$baseUrl/', headers=headers)" else "\nr = requests.get('$baseUrl/')"
                val pythonExample = "import requests$pythonHeaders\nprint(r.json())"

                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Examples", style = MaterialTheme.typography.titleSmall)
                        CodeBlock("curl", curlExample) {
                            clipboard.setText(AnnotatedString(curlExample))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        CodeBlock("Node.js (fetch)", fetchExample) {
                            clipboard.setText(AnnotatedString(fetchExample))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        CodeBlock("Python (requests)", pythonExample) {
                            clipboard.setText(AnnotatedString(pythonExample))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
