package com.hcwebhook.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import com.hcwebhook.app.LocalHttpServerService
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalHttpSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var localTcpEnabled by remember { mutableStateOf(preferencesManager.isLocalTcpEnabled()) }
    var localTcpPort by remember { mutableStateOf(preferencesManager.getLocalTcpPort().toString()) }
    BackHandler(onBack = onBack)

    // Runtime notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently; service already started */ }

    fun startServerWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        LocalHttpServerService.start(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_local_http_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.config_local_tcp_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = localTcpEnabled,
                            onCheckedChange = { enabled ->
                                localTcpEnabled = enabled
                                preferencesManager.setLocalTcpEnabled(enabled)
                                if (enabled) {
                                    startServerWithPermission()
                                } else {
                                    LocalHttpServerService.stop(context)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = localTcpPort,
                        onValueChange = { localTcpPort = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.config_local_tcp_port_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
                            if (localTcpEnabled) {
                                // Restart service so server rebinds to updated port.
                                LocalHttpServerService.stop(context)
                                LocalHttpServerService.start(context)
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.config_local_tcp_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.action_save))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (localTcpEnabled) {
                            stringResource(
                                R.string.config_local_tcp_endpoint_enabled,
                                preferencesManager.getLocalTcpPort()
                            )
                        } else {
                            stringResource(R.string.config_local_tcp_endpoint_disabled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
