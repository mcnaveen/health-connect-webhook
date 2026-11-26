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
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import com.hcwebhook.app.AppLock
import com.hcwebhook.app.applyRecentsPreview
import androidx.compose.material.icons.filled.Palette
import com.hcwebhook.app.ui.AppearanceState
import com.hcwebhook.app.ui.Group
import com.hcwebhook.app.ui.SettingRow
import com.hcwebhook.app.ui.SwitchRow
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
            Text(
                stringResource(R.string.nav_about),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
            )
            Group(title = stringResource(R.string.settings_section_sync)) {
                item {
                    SettingRow(
                        icon = Icons.Filled.Hub,
                        title = stringResource(R.string.integrations_title),
                        summary = stringResource(
                            R.string.integrations_summary,
                            stringResource(if (localHttpEnabled) R.string.state_on else R.string.state_off),
                            stringResource(if (preferencesManager.isMcpEnabled()) R.string.state_on else R.string.state_off),
                        ),
                        chevron = true,
                        onClick = onOpenLocalHttpSettings,
                    )
                }
                item {
                    SettingRow(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.about_notifications_title),
                        summary = stringResource(R.string.about_notifications_desc),
                        chevron = true,
                        onClick = onOpenNotificationsSettings,
                    )
                }
                item { TimestampStyleRow(preferencesManager) }
            }

            Group(title = stringResource(R.string.settings_section_app), modifier = Modifier.padding(top = 20.dp)) {
                item { LanguageRow() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        SwitchRow(
                            icon = Icons.Filled.Palette,
                            title = stringResource(R.string.settings_dynamic_color_title),
                            summary = stringResource(R.string.settings_dynamic_color_desc),
                            checked = AppearanceState.dynamicColor.value,
                            onChange = { preferencesManager.setDynamicColor(it) },
                        )
                    }
                }
                item {
                    var appLock by remember { mutableStateOf(preferencesManager.isAppLockEnabled()) }
                    SwitchRow(
                        icon = Icons.Filled.Lock,
                        title = stringResource(R.string.app_lock_title),
                        summary = stringResource(R.string.app_lock_desc),
                        checked = appLock,
                        onChange = { on ->
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (!on) {
                                preferencesManager.setAppLockEnabled(false)
                                appLock = false
                                activity?.let(::applyRecentsPreview)
                            } else if (activity == null || !AppLock.canUse(context)) {
                                Toast.makeText(context, R.string.app_lock_no_credential, Toast.LENGTH_LONG).show()
                            } else {
                                // Confirm first, so nobody turns on a lock that they cannot open.
                                AppLock.authenticate(activity, context.getString(R.string.app_lock_confirm)) { ok ->
                                    if (ok) {
                                        preferencesManager.setAppLockEnabled(true)
                                        appLock = true
                                        applyRecentsPreview(activity)
                                    }
                                }
                            }
                        },
                    )
                }
                item {
                    SettingRow(
                        icon = Icons.Filled.Backup,
                        title = stringResource(R.string.about_settings_backup_title),
                        summary = stringResource(R.string.about_settings_backup_desc),
                        chevron = true,
                        onClick = onOpenSettingsBackup,
                    )
                }
            }

            Group(title = stringResource(R.string.settings_section_help), modifier = Modifier.padding(top = 20.dp)) {
                item {
                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        title = stringResource(R.string.about_link_changelog),
                        summary = stringResource(R.string.about_link_changelog_desc),
                        chevron = true,
                        onClick = onOpenChangelog,
                    )
                }
                item {
                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = stringResource(R.string.about_link_feedback),
                        summary = stringResource(R.string.about_feedback_desc),
                        chevron = true,
                        onClick = { showFeedbackSheet = true },
                    )
                }
                item {
                    SettingRow(
                        icon = Icons.Filled.PlayArrow,
                        title = stringResource(R.string.about_link_intro),
                        chevron = true,
                        onClick = onRestartOnboarding,
                    )
                }
                if (!FlavorUtils.isPlayStore) {
                    item {
                        SettingRow(
                            icon = Icons.Filled.Code,
                            title = stringResource(R.string.about_link_github),
                            trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mcnaveen/health-connect-webhook"))
                                context.startActivity(intent)
                            },
                        )
                    }
                }
            }

            Group(title = stringResource(R.string.settings_section_about), modifier = Modifier.padding(top = 20.dp)) {
                item {
                    TextBlock(
                        title = stringResource(R.string.about_privacy_title),
                        // One bullet per line instead of one long paragraph.
                        body = stringResource(R.string.about_privacy_desc).replace(" • ", "\n• "),
                    )
                }
                item {
                    TextBlock(
                        title = stringResource(R.string.medical_disclaimer_title),
                        body = stringResource(R.string.medical_disclaimer_desc),
                    )
                }
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
        SettingRow(
            icon = Icons.Filled.Schedule,
            title = stringResource(R.string.settings_timestamps_title),
            summary = stringResource(
                R.string.settings_timestamps_example,
                time.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
                time.timezoneName,
            ),
            value = stringResource(label(style)),
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
        "it" to "Italiano", // Italian
        "ar" to "العربية" // Arabic
    )
    val currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        .get(0)?.language.orEmpty()
    val currentName = languages.firstOrNull { it.first == currentTag }?.second ?: systemDefault

    Box {
        SettingRow(
            icon = Icons.Filled.Translate,
            title = stringResource(R.string.settings_language_title),
            value = currentName,
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
