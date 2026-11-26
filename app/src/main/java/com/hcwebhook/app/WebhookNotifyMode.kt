package com.hcwebhook.app

import kotlinx.serialization.Serializable

@Serializable
enum class WebhookNotifyMode {
    // Notify after every sync.
    ALL,
    // Notify after every failed sync.
    FAILURES_ONLY,
    // Notify once when the webhook starts to fail, and once when it works again.
    FAILURE_AND_RECOVERY
}
