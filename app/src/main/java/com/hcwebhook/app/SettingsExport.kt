package com.hcwebhook.app

import kotlinx.serialization.Serializable

/**
 * Data class representing a full settings snapshot that can be exported/imported as JSON.
 */
@Serializable
data class SettingsExport(
    val appVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val webhookConfigs: List<WebhookConfig> = emptyList(),
    val enabledDataTypes: List<String> = emptyList(),
    val syncMode: String = SyncMode.INTERVAL.name,
    val syncIntervalMinutes: Int = 60,
    val scheduledSyncs: List<ScheduledSync> = emptyList()
)
