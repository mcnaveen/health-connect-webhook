package com.hcwebhook.app

import kotlinx.serialization.Serializable

@Serializable
data class WebhookConfig(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    // null = send all globally-enabled data types; non-null = send only these types
    val dataTypeFilter: Set<String>? = null,
    // Optional: IDs referencing global NotificationConfigs from PreferencesManager
    val notificationConfigIds: Set<String> = emptySet()
) {
    fun getHeaderCount(): Int = headers.size

    fun withHeader(key: String, value: String): WebhookConfig {
        return copy(headers = headers + (key to value))
    }

    fun withoutHeader(key: String): WebhookConfig {
        return copy(headers = headers - key)
    }

    companion object {
        fun fromUrl(url: String): WebhookConfig {
            return WebhookConfig(url = url, headers = emptyMap())
        }
    }
}
