package com.hcwebhook.app

import kotlinx.serialization.Serializable

@Serializable
data class WebhookLog(
    val id: String,
    val timestamp: Long,
    val url: String,
    val statusCode: Int?,
    val success: Boolean,
    val errorMessage: String?,
    val dataType: String?,
    val recordCount: Int?
)
