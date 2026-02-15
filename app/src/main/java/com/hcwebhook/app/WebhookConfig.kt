package com.hcwebhook.app

import kotlinx.serialization.Serializable

/**
 * Configuration for a webhook endpoint including URL and custom headers
 */
@Serializable
data class WebhookConfig(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) {
    /**
     * Returns the header count for UI display
     */
    fun getHeaderCount(): Int = headers.size

    /**
     * Adds or updates a header
     */
    fun withHeader(key: String, value: String): WebhookConfig {
        return copy(headers = headers + (key to value))
    }

    /**
     * Removes a header
     */
    fun withoutHeader(key: String): WebhookConfig {
        return copy(headers = headers - key)
    }

    companion object {
        /**
         * Creates a WebhookConfig from a plain URL string
         */
        fun fromUrl(url: String): WebhookConfig {
            return WebhookConfig(url = url, headers = emptyMap())
        }
    }
}
