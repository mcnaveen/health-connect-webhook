package com.hcwebhook.app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WebhookConfigTest {

    @Test
    fun legacyConfig_withoutNotifyMode_isAll() {
        val configs = Json.decodeFromString<List<WebhookConfig>>(
            """[{"url":"https://a.test","notificationConfigIds":["n1"]}]"""
        )

        assertEquals(WebhookNotifyMode.ALL, configs.single().notifyMode)
    }

    @Test
    fun legacyBackup_withoutNotifyMode_isAll() {
        val export = Json { ignoreUnknownKeys = true }.decodeFromString<SettingsExport>(
            """{"appVersion":1,"webhookConfigs":[{"url":"https://a.test"}]}"""
        )

        assertEquals(WebhookNotifyMode.ALL, export.webhookConfigs.single().notifyMode)
    }

    @Test
    fun notifyMode_survivesRoundTrip() {
        val config = WebhookConfig(url = "https://a.test", notifyMode = WebhookNotifyMode.FAILURE_AND_RECOVERY)
        val decoded = Json.decodeFromString<WebhookConfig>(Json.encodeToString(config))

        assertEquals(WebhookNotifyMode.FAILURE_AND_RECOVERY, decoded.notifyMode)
    }

    @Test
    fun defaultNotifyMode_isNotWritten() {
        val json = Json.encodeToString(WebhookConfig(url = "https://a.test"))

        assertFalse(json.contains("notifyMode"))
    }
}
