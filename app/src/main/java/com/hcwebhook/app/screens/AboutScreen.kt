package com.hcwebhook.app.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.BuildConfig
import com.hcwebhook.app.FlavorUtils
import com.hcwebhook.app.PayloadTime
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.R
import com.hcwebhook.app.TimestampStyle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AboutScreen(
    onRestartOnboarding: () -> Unit = {},
    onOpenLocalHttpSettings: () -> Unit = {},
    onOpenNotificationsSettings: () -> Unit = {},
    onOpenSettingsBackup: () -> Unit = {},
    onOpenChangelog: () -> Unit = {},
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    val feedbackSuccessMessage = stringResource(R.string.feedback_success)

    // ── Version info ──────────────────────────────────────────────────────────
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) { "Unknown" }

    val versionCode = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) { 0L }

    // The server screen is a separate destination, so this reads fresh values on return.
    val localHttpEnabled = remember { preferencesManager.isLocalTcpEnabled() }
    val localHttpPort = remember { preferencesManager.getLocalTcpPort() }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_sync))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Filled.Lan,
                    title = stringResource(R.string.about_local_http_title),
                    subtitle = if (localHttpEnabled) {
                        stringResource(R.string.settings_local_http_on, localHttpPort)
                    } else {
                        stringResource(R.string.settings_local_http_off)
                    },
                    onClick = onOpenLocalHttpSettings,
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.about_notifications_title),
                    subtitle = stringResource(R.string.about_notifications_desc),
                    onClick = onOpenNotificationsSettings,
                )
                RowDivider()
                TimestampStyleRow(preferencesManager)
            }

            SectionLabel(stringResource(R.string.settings_section_app))
            SettingsGroup {
                LanguageRow()
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.Backup,
                    title = stringResource(R.string.about_settings_backup_title),
                    subtitle = stringResource(R.string.about_settings_backup_desc),
                    onClick = onOpenSettingsBackup,
                )
            }

            SectionLabel(stringResource(R.string.settings_section_help))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    title = stringResource(R.string.about_link_changelog),
                    subtitle = stringResource(R.string.about_link_changelog_desc),
                    onClick = onOpenChangelog,
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = stringResource(R.string.about_link_feedback),
                    subtitle = stringResource(R.string.about_feedback_desc),
                    onClick = { showFeedbackSheet = true },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.about_link_intro),
                    onClick = onRestartOnboarding,
                )
                if (!FlavorUtils.isPlayStore) {
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Code,
                        title = stringResource(R.string.about_link_github),
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mcnaveen/health-connect-webhook"))
                            context.startActivity(intent)
                        },
                    )
                }
            }

            SectionLabel(stringResource(R.string.settings_section_about))
            SettingsGroup {
                TextBlock(
                    title = stringResource(R.string.about_privacy_title),
                    // One bullet per line instead of one long paragraph.
                    body = stringResource(R.string.about_privacy_desc).replace(" • ", "\n• "),
                )
                RowDivider(startPadding = 16.dp)
                TextBlock(
                    title = stringResource(R.string.medical_disclaimer_title),
                    body = stringResource(R.string.medical_disclaimer_desc),
                )
            }

            // ── Version footer ───────────────────────────────────────────────
            Text(
                text = stringResource(R.string.about_app_title) + "\n" +
                    stringResource(R.string.about_app_version, versionName, versionCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .then(
                        if (BuildConfig.DEBUG) {
                            Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    preferencesManager.debugForceWeeklyFeedbackPrompt()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.feedback_weekly_debug_forced),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            )
                        } else {
                            Modifier
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for nav bar
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        FeedbackSheet(
            visible = showFeedbackSheet,
            onDismiss = { showFeedbackSheet = false },
            onSubmitted = {
                scope.launch {
                    snackbarHostState.showSnackbar(feedbackSuccessMessage)
                }
            },
        )
    }
}

// Two text sizes only: bodyLarge for row titles, bodyMedium for everything else.
// Section labels and block titles differ by weight, not size.

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider(startPadding: androidx.compose.ui.unit.Dp = 56.dp) {
    // Starts under the text, not the icon.
    HorizontalDivider(
        modifier = Modifier.padding(start = startPadding),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
    trailingIcon: ImageVector? = Icons.Filled.ChevronRight,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TextBlock(title: String, body: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** UTC or local time for every time in the JSON payload. */
@Composable
private fun TimestampStyleRow(preferencesManager: PreferencesManager) {
    var style by remember { mutableStateOf(preferencesManager.getTimestampStyle()) }
    var expanded by remember { mutableStateOf(false) }
    val time = PayloadTime(style)
    fun label(option: TimestampStyle) = when (option) {
        TimestampStyle.UTC -> R.string.settings_timestamps_utc
        TimestampStyle.LOCAL -> R.string.settings_timestamps_local
    }

    Box {
        SettingsRow(
            icon = Icons.Filled.Schedule,
            title = stringResource(R.string.settings_timestamps_title),
            subtitle = stringResource(
                R.string.settings_timestamps_example,
                time.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
                time.timezoneName,
            ),
            value = stringResource(label(style)),
            trailingIcon = null,
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = stringResource(R.string.settings_timestamps_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TimestampStyle.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(label(option))) },
                    trailingIcon = { if (option == style) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        preferencesManager.setTimestampStyle(option)
                        style = option
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow() {
    var expanded by remember { mutableStateOf(false) }
    val systemDefault = stringResource(R.string.settings_language_system)
    val languages = listOf(
        "" to systemDefault,
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
    val currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        .get(0)?.language.orEmpty()
    val currentName = languages.firstOrNull { it.first == currentTag }?.second ?: systemDefault

    Box {
        SettingsRow(
            icon = Icons.Filled.Translate,
            title = stringResource(R.string.settings_language_title),
            value = currentName,
            trailingIcon = null,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (tag, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    trailingIcon = { if (tag == currentTag) Icon(Icons.Filled.Check, contentDescription = null) },
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
