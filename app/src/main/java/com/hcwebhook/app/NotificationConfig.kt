package com.hcwebhook.app

import kotlinx.serialization.Serializable

enum class NotificationProviderType {
    GOTIFY, NTFY, TELEGRAM, DISCORD, PUSHOVER, CUSTOM_HTTP;

    val displayName: String
        get() = when (this) {
            GOTIFY       -> "Gotify"
            NTFY         -> "ntfy"
            TELEGRAM     -> "Telegram"
            DISCORD      -> "Discord"
            PUSHOVER     -> "Pushover"
            CUSTOM_HTTP  -> "Custom HTTP"
        }

    val description: String
        get() = when (this) {
            GOTIFY       -> "Self-hosted push notifications"
            NTFY         -> "Simple HTTP-based pub-sub"
            TELEGRAM     -> "Message via Telegram bot"
            DISCORD      -> "Discord webhook message"
            PUSHOVER     -> "Pushover push notification"
            CUSTOM_HTTP  -> "Custom HTTP POST request"
        }
}

@Serializable
data class NotificationConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isEnabled: Boolean = true,
    val providerType: NotificationProviderType = NotificationProviderType.GOTIFY,
    // Gotify / ntfy / Discord / Custom HTTP: server URL or webhook URL
    val url: String = "",
    // Gotify: access token | Telegram: bot token | Pushover: app token
    val token: String = "",
    // ntfy: topic name
    val topic: String = "",
    // Telegram: chat ID
    val chatId: String = "",
    // Pushover: user/group key
    val userKey: String = "",
    // Custom HTTP: extra request headers
    val headers: Map<String, String> = emptyMap(),
    // Custom HTTP: body template — supports {title} {message} {status}
    val bodyTemplate: String = ""
) {
    val displayIdentifier: String
        get() = when (providerType) {
            NotificationProviderType.GOTIFY -> url.ifEmpty { "Configured" }
            NotificationProviderType.NTFY -> if (url.isNotEmpty() && topic.isNotEmpty()) "$url/$topic" else if (url.isNotEmpty()) url else if (topic.isNotEmpty()) topic else "Configured"
            NotificationProviderType.TELEGRAM -> if (chatId.isNotEmpty()) "Chat: $chatId" else "Configured"
            NotificationProviderType.DISCORD -> url.ifEmpty { "Configured" }
            NotificationProviderType.PUSHOVER -> if (userKey.isNotEmpty()) "User: $userKey" else "Configured"
            NotificationProviderType.CUSTOM_HTTP -> url.ifEmpty { "Configured" }
        }
}
