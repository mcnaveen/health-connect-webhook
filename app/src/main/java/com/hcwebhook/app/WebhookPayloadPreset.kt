package com.hcwebhook.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WebhookPayloadPreset {
    @SerialName("default")
    DEFAULT,

    @SerialName("camel_case")
    CAMEL_CASE,

    @SerialName("open_wearables")
    OPEN_WEARABLES
}
