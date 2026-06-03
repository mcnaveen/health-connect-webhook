package com.hcwebhook.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class SyncMode {
    INTERVAL,    // Continuous sync at specified interval
    SCHEDULED    // Sync at specific times (morning & evening)
}

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "hc_webhook_prefs"
        private const val KEY_LAST_SYNC_TS_PREFIX = "last_sync_ts_"
        private const val KEY_LAST_STEPS_SYNC_TS = "last_steps_sync_ts"
        private const val KEY_LAST_SLEEP_SYNC_TS = "last_sleep_sync_ts"
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_WEBHOOK_URLS = "webhook_urls"
        private const val KEY_WEBHOOK_CONFIGS = "webhook_configs"
        private const val KEY_ENABLED_DATA_TYPES = "enabled_data_types"
        private const val KEY_WEBHOOK_LOGS = "webhook_logs"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_SUMMARY = "last_sync_summary"
        private const val DEFAULT_SYNC_INTERVAL_MINUTES = 60
        private const val MAX_LOGS = 100
        private const val KEY_SCHEDULED_SYNC_ENABLED = "scheduled_sync_enabled"
        private const val KEY_MORNING_SYNC_HOUR = "morning_sync_hour"
        private const val KEY_MORNING_SYNC_MINUTE = "morning_sync_minute"
        private const val KEY_EVENING_SYNC_HOUR = "evening_sync_hour"
        private const val KEY_EVENING_SYNC_MINUTE = "evening_sync_minute"
        private const val KEY_SYNC_MODE = "sync_mode"
        private const val KEY_SCHEDULED_SYNCS = "scheduled_syncs"
        private const val KEY_KNOWN_GRANTED_PERMISSIONS = "known_granted_permissions"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        private const val KEY_LOCAL_TCP_ENABLED = "local_tcp_enabled"
        private const val KEY_LOCAL_TCP_PORT = "local_tcp_port"
        private const val DEFAULT_LOCAL_TCP_PORT = 8787
        private const val KEY_LOCAL_HTTP_AUTH_ENABLED = "local_http_auth_enabled"
        private const val KEY_LOCAL_HTTP_TOKEN = "local_http_token"
        private const val KEY_NOTIFICATION_CONFIGS = "notification_configs"
        private const val KEY_RAW_STEPS_ENABLED = "raw_steps_enabled"
    }


    fun getLastStepsSyncTimestamp(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_STEPS_SYNC_TS, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastStepsSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_STEPS_SYNC_TS, timestamp).apply()
    }

    fun getLastSleepSyncTimestamp(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SLEEP_SYNC_TS, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSleepSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SLEEP_SYNC_TS, timestamp).apply()
    }

    fun getSyncIntervalMinutes(): Int {
        return prefs.getInt(KEY_SYNC_INTERVAL_MINUTES, DEFAULT_SYNC_INTERVAL_MINUTES)
    }

    fun setSyncIntervalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SYNC_INTERVAL_MINUTES, minutes).apply()
    }

    fun getWebhookUrls(): List<String> {
        val urlsString = prefs.getString(KEY_WEBHOOK_URLS, "") ?: ""
        return if (urlsString.isEmpty()) emptyList() else urlsString.split(",")
    }

    fun setWebhookUrls(urls: List<String>) {
        val urlsString = urls.joinToString(",")
        prefs.edit().putString(KEY_WEBHOOK_URLS, urlsString).apply()
    }

    fun getWebhookConfigs(): List<WebhookConfig> {
        val configsJson = prefs.getString(KEY_WEBHOOK_CONFIGS, null)
        
        // If we have new format configs, use them
        if (configsJson != null) {
            return try {
                Json.decodeFromString<List<WebhookConfig>>(configsJson)
            } catch (e: Exception) {
                // If JSON parsing fails, fall back to old format
                getWebhookUrls().map { WebhookConfig.fromUrl(it) }
            }
        }
        
        // Fall back to old format for backward compatibility
        return getWebhookUrls().map { WebhookConfig.fromUrl(it) }
    }

    fun setWebhookConfigs(configs: List<WebhookConfig>) {
        val configsJson = Json.encodeToString(configs)
        prefs.edit().putString(KEY_WEBHOOK_CONFIGS, configsJson).apply()
        
        // Also update old format for backward compatibility
        val urls = configs.map { it.url }
        setWebhookUrls(urls)
    }


    fun getEnabledDataTypes(): Set<HealthDataType> {
        val typesString = prefs.getString(KEY_ENABLED_DATA_TYPES, "") ?: ""
        return if (typesString.isEmpty()) {
            emptySet()
        } else {
            typesString.split(",").mapNotNull {
                try { HealthDataType.valueOf(it) } catch (e: Exception) { null }
            }.toSet()
        }
    }

    fun setEnabledDataTypes(types: Set<HealthDataType>) {
        val typesString = types.joinToString(",") { it.name }
        prefs.edit().putString(KEY_ENABLED_DATA_TYPES, typesString).apply()
    }

    fun getKnownGrantedPermissions(): Set<String> {
        return prefs.getStringSet(KEY_KNOWN_GRANTED_PERMISSIONS, emptySet()) ?: emptySet()
    }

    fun setKnownGrantedPermissions(permissions: Set<String>) {
        prefs.edit().putStringSet(KEY_KNOWN_GRANTED_PERMISSIONS, permissions).apply()
    }

    fun getLastSyncTimestamp(type: HealthDataType): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SYNC_TS_PREFIX + type.name, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSyncTimestamp(type: HealthDataType, timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TS_PREFIX + type.name, timestamp).apply()
    }

    fun getWebhookLogs(): List<WebhookLog> {
        val logsJson = prefs.getString(KEY_WEBHOOK_LOGS, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<WebhookLog>>(logsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addWebhookLog(log: WebhookLog) {
        val currentLogs = getWebhookLogs().toMutableList()
        currentLogs.add(0, log) // Add to beginning

        // Keep only the most recent MAX_LOGS entries
        val trimmedLogs = currentLogs.take(MAX_LOGS)

        val logsJson = Json.encodeToString(trimmedLogs)
        prefs.edit().putString(KEY_WEBHOOK_LOGS, logsJson).apply()
    }

    fun removeWebhookLog(id: String) {
        val updated = getWebhookLogs().filter { it.id != id }
        val logsJson = Json.encodeToString(updated)
        prefs.edit().putString(KEY_WEBHOOK_LOGS, logsJson).apply()
    }

    fun removeWebhookLogs(ids: Set<String>) {
        val updated = getWebhookLogs().filter { it.id !in ids }
        prefs.edit().putString(KEY_WEBHOOK_LOGS, Json.encodeToString(updated)).apply()
    }

    fun clearWebhookLogs() {
        prefs.edit().remove(KEY_WEBHOOK_LOGS).apply()
    }

    fun isScheduledSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCHEDULED_SYNC_ENABLED, true)
    }

    fun setScheduledSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULED_SYNC_ENABLED, enabled).apply()
    }

    fun getMorningSyncHour(): Int = prefs.getInt(KEY_MORNING_SYNC_HOUR, 8)
    fun getMorningSyncMinute(): Int = prefs.getInt(KEY_MORNING_SYNC_MINUTE, 0)

    fun setMorningSyncTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_MORNING_SYNC_HOUR, hour)
            .putInt(KEY_MORNING_SYNC_MINUTE, minute)
            .apply()
    }

    fun getEveningSyncHour(): Int = prefs.getInt(KEY_EVENING_SYNC_HOUR, 21)
    fun getEveningSyncMinute(): Int = prefs.getInt(KEY_EVENING_SYNC_MINUTE, 0)

    fun setEveningSyncTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_EVENING_SYNC_HOUR, hour)
            .putInt(KEY_EVENING_SYNC_MINUTE, minute)
            .apply()
    }

    fun getSyncMode(): SyncMode {
        val modeString = prefs.getString(KEY_SYNC_MODE, SyncMode.INTERVAL.name) ?: SyncMode.INTERVAL.name
        return try {
            SyncMode.valueOf(modeString)
        } catch (e: Exception) {
            SyncMode.INTERVAL // Default to interval mode for backward compatibility
        }
    }

    fun setSyncMode(mode: SyncMode) {
        prefs.edit().putString(KEY_SYNC_MODE, mode.name).apply()
    }

    fun getScheduledSyncs(): List<ScheduledSync> {
        val syncsJson = prefs.getString(KEY_SCHEDULED_SYNCS, null)
        
        if (syncsJson != null) {
            return try {
                Json.decodeFromString<List<ScheduledSync>>(syncsJson)
            } catch (e: Exception) {
                // If JSON parsing fails, return default schedules
                getDefaultScheduledSyncs()
            }
        }
        
        // For backward compatibility, migrate from old morning/evening settings
        val morningHour = getMorningSyncHour()
        val morningMinute = getMorningSyncMinute()
        val eveningHour = getEveningSyncHour()
        val eveningMinute = getEveningSyncMinute()
        
        // Check if these are default values (8:00 and 21:00)
        val isDefaultValues = morningHour == 8 && morningMinute == 0 && eveningHour == 21 && eveningMinute == 0
        
        return if (isDefaultValues) {
            getDefaultScheduledSyncs()
        } else {
            // Migrate user's custom times
            listOf(
                ScheduledSync.create(morningHour, morningMinute, "Morning"),
                ScheduledSync.create(eveningHour, eveningMinute, "Evening")
            )
        }
    }

    fun setScheduledSyncs(syncs: List<ScheduledSync>) {
        val syncsJson = Json.encodeToString(syncs)
        prefs.edit().putString(KEY_SCHEDULED_SYNCS, syncsJson).apply()
    }
    
    private fun getDefaultScheduledSyncs(): List<ScheduledSync> {
        return listOf(
            ScheduledSync.create(8, 0, "Morning"),
            ScheduledSync.create(21, 0, "Evening")
        )
    }

    fun getLastSyncTime(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SYNC_TIME, -1)
        return if (timestamp == -1L) null else timestamp
    }

    fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }

    fun getLastSyncSummary(): String? {
        return prefs.getString(KEY_LAST_SYNC_SUMMARY, null)
    }

    fun setLastSyncSummary(summary: String) {
        prefs.edit().putString(KEY_LAST_SYNC_SUMMARY, summary).apply()
    }

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun setHasSeenOnboarding() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
    }

    fun getLastSeenVersionCode(): Int =
        prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, 0)

    fun setLastSeenVersionCode(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, versionCode).apply()
    }

    fun isLocalTcpEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_TCP_ENABLED, false)
    }

    fun setLocalTcpEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_TCP_ENABLED, enabled).apply()
    }

    fun getLocalTcpPort(): Int {
        val configuredPort = prefs.getInt(KEY_LOCAL_TCP_PORT, DEFAULT_LOCAL_TCP_PORT)
        return if (configuredPort in 1024..65535) configuredPort else DEFAULT_LOCAL_TCP_PORT
    }

    fun setLocalTcpPort(port: Int) {
        val safePort = port.coerceIn(1024, 65535)
        prefs.edit().putInt(KEY_LOCAL_TCP_PORT, safePort).apply()
    }

    fun isLocalHttpAuthEnabled(): Boolean = prefs.getBoolean(KEY_LOCAL_HTTP_AUTH_ENABLED, false)

    fun setLocalHttpAuthEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_HTTP_AUTH_ENABLED, enabled).apply()
    }

    fun getLocalHttpToken(): String {
        val existing = prefs.getString(KEY_LOCAL_HTTP_TOKEN, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_LOCAL_HTTP_TOKEN, generated).apply()
        return generated
    }

    fun regenerateLocalHttpToken(): String {
        val token = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_LOCAL_HTTP_TOKEN, token).apply()
        return token
    }

    fun getNotificationConfigs(): List<NotificationConfig> {
        val configsJson = prefs.getString(KEY_NOTIFICATION_CONFIGS, null)
        if (configsJson != null) {
            return try {
                Json.decodeFromString<List<NotificationConfig>>(configsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
        return emptyList()
    }

    fun setNotificationConfigs(configs: List<NotificationConfig>) {
        val configsJson = Json.encodeToString(configs)
        prefs.edit().putString(KEY_NOTIFICATION_CONFIGS, configsJson).apply()
    }

    /**
     * When enabled, step data is sent as raw Health Connect intervals (granular,
     * non-cumulative) instead of a single per-day cumulative total.
     * Defaults to false to preserve the existing per-day aggregate behavior.
     */
    fun isRawStepsEnabled(): Boolean = prefs.getBoolean(KEY_RAW_STEPS_ENABLED, false)

    fun setRawStepsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RAW_STEPS_ENABLED, enabled).apply()
    }

    // -------------------------------------------------------------------------
    // Export / Import
    // -------------------------------------------------------------------------

    /**
     * Builds a [SettingsExport] snapshot of all current user-configurable preferences.
     */
    fun exportSettings(): SettingsExport {
        return SettingsExport(
            exportedAt = System.currentTimeMillis(),
            webhookConfigs = getWebhookConfigs(),
            enabledDataTypes = getEnabledDataTypes().map { it.name },
            syncMode = getSyncMode().name,
            syncIntervalMinutes = getSyncIntervalMinutes(),
            scheduledSyncs = getScheduledSyncs(),
            localTcpEnabled = isLocalTcpEnabled(),
            localTcpPort = getLocalTcpPort(),
            notificationConfigs = getNotificationConfigs(),
            rawStepsEnabled = isRawStepsEnabled()
        )
    }

    /**
     * Restores all user-configurable preferences from a [SettingsExport].
     */
    fun importSettings(export: SettingsExport) {
        setWebhookConfigs(export.webhookConfigs)

        val dataTypes = export.enabledDataTypes.mapNotNull { name ->
            try { HealthDataType.valueOf(name) } catch (e: Exception) { null }
        }.toSet()
        setEnabledDataTypes(dataTypes)

        val mode = try { SyncMode.valueOf(export.syncMode) } catch (e: Exception) { SyncMode.INTERVAL }
        setSyncMode(mode)

        setSyncIntervalMinutes(export.syncIntervalMinutes)
        setScheduledSyncs(export.scheduledSyncs)
        setLocalTcpEnabled(export.localTcpEnabled)
        setLocalTcpPort(export.localTcpPort)
        setNotificationConfigs(export.notificationConfigs)
        setRawStepsEnabled(export.rawStepsEnabled)
    }
}