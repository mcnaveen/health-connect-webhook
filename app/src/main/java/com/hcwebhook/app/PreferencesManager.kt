package com.hcwebhook.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "hc_webhook_prefs"
        private const val KEY_LAST_SYNC_TS_PREFIX = "last_sync_ts_"
        private const val KEY_LAST_STEPS_SYNC_TS = "last_steps_sync_ts"
        private const val KEY_LAST_SLEEP_SYNC_TS = "last_sleep_sync_ts"
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_WEBHOOK_URLS = "webhook_urls"
        private const val KEY_ENABLED_DATA_TYPES = "enabled_data_types"
        private const val KEY_WEBHOOK_LOGS = "webhook_logs"
        private const val DEFAULT_SYNC_INTERVAL_MINUTES = 60
        private const val MAX_LOGS = 100
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

    fun clearWebhookLogs() {
        prefs.edit().remove(KEY_WEBHOOK_LOGS).apply()
    }
}