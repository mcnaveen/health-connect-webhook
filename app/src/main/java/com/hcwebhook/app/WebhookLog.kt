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
    val recordCount: Int?,
    val responseTimeMs: Long? = null,
    val syncType: String? = null,
    val payload: String? = null,
    /** "json" | "grpc". Null on older logs — inferred from [payload] when needed. */
    val deliveryFormat: String? = null
) {
    fun resolvedDeliveryFormat(): String {
        when (deliveryFormat) {
            "grpc" -> return "grpc"
            "json" -> return "json"
        }
        // Legacy logs written before deliveryFormat existed
        if (payload?.startsWith("grpc ") == true) return "grpc"
        return "json"
    }

    companion object {
        const val FORMAT_JSON = "json"
        const val FORMAT_GRPC = "grpc"
    }
}
