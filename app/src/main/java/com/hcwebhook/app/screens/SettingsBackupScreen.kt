package com.hcwebhook.app.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import com.hcwebhook.app.SettingsExport
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val prettyJson = Json { prettyPrint = true }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open file")
                val jsonText = inputStream.bufferedReader().use { it.readText() }
                val export = Json.decodeFromString<SettingsExport>(jsonText)
                prefsManager.importSettings(export)
                Toast.makeText(context, context.getString(R.string.about_toast_import_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                val msg = e.message ?: context.getString(R.string.about_error_unknown)
                Toast.makeText(context, context.getString(R.string.about_toast_import_failed, msg), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportSettings() {
        try {
            val export = prefsManager.exportSettings()
            val jsonText = prettyJson.encodeToString(export)
            val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportFile = File(exportDir, "hc_webhook_settings_$timestamp.json")
            exportFile.writeText(jsonText)
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "HC Webhook Settings Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.about_export_intent_title)))
        } catch (e: Exception) {
            val msg = e.message ?: context.getString(R.string.about_error_unknown)
            Toast.makeText(context, context.getString(R.string.about_toast_export_failed, msg), Toast.LENGTH_LONG).show()
        }
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text(stringResource(R.string.about_import_sheet_title)) },
            text = { Text(stringResource(R.string.about_import_sheet_desc)) },
            confirmButton = {
                Button(onClick = {
                    showImportConfirmDialog = false
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Text(stringResource(R.string.about_action_choose_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_settings_backup_title)) },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.about_settings_backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { exportSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.about_action_export))
            }
            OutlinedButton(
                onClick = { showImportConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.about_action_import))
            }
        }
    }
}
