package com.hcwebhook.app.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.FlavorUtils
import com.hcwebhook.app.R
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import com.hcwebhook.app.ui.theme.IconBackgroundBlue
import com.hcwebhook.app.ui.theme.IconBackgroundGreen
import com.hcwebhook.app.ui.theme.IconTintBlue
import com.hcwebhook.app.ui.theme.IconTintGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onRestartOnboarding: () -> Unit = {},
    onOpenLocalHttpSettings: () -> Unit = {},
    onOpenNotificationsSettings: () -> Unit = {},
    onOpenSettingsBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Version info ──────────────────────────────────────────────────────────
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) { "Unknown" }

    val versionCode = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) { 0L }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(IconBackgroundBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = IconTintBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.about_app_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.about_app_version, versionName, versionCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Description Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.about_app_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    LinkRow(
                        label = stringResource(R.string.about_local_http_title),
                        subtitle = stringResource(R.string.config_local_tcp_desc),
                        icon = Icons.Filled.Lan,
                        onClick = { onOpenLocalHttpSettings() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    LinkRow(
                        label = stringResource(R.string.about_notifications_title),
                        subtitle = stringResource(R.string.about_notifications_desc),
                        icon = Icons.Filled.Add,
                        onClick = { onOpenNotificationsSettings() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    LinkRow(
                        label = stringResource(R.string.about_settings_backup_title),
                        subtitle = stringResource(R.string.about_settings_backup_desc),
                        icon = Icons.Filled.Backup,
                        onClick = { onOpenSettingsBackup() }
                    )
                }
            }

            LanguageSelectorCard()

                        // Privacy & Security Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(IconBackgroundGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = IconTintGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.about_privacy_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.about_privacy_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.about_links_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                    )

                    if (!FlavorUtils.isPlayStore) {
                        LinkRow(
                            label = stringResource(R.string.about_link_github),
                            icon = Icons.Filled.Code,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mcnaveen/health-connect-webhook"))
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    LinkRow(
                        label = "Provide Feedback",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hc-webhook.feedbackjar.com/"))
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    LinkRow(
                        label = "View Introduction",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onRestartOnboarding() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for nav bar

        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LinkRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}


@Composable
private fun LanguageSelectorCard() {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf(
        "" to "System Default",
        "en" to "English",
        "ta" to "தமிழ்", // Tamil
        "fr" to "Français", // French
        "de" to "Deutsch", // German
        "es" to "Español", // Spanish
        "pt" to "Português", // Portuguese
        "zh" to "中文", // Chinese
        "ja" to "日本語", // Japanese
        "ko" to "한국어", // Korean
        "it" to "Italiano" // Italian
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Language",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Change the application language context",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { (tag, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            if (tag.isEmpty()) {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.getEmptyLocaleList())
                            } else {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(tag))
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
