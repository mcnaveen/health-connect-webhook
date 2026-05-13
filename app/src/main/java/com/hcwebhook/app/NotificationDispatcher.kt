package com.hcwebhook.app

import android.content.Context

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Fires a best-effort HTTP notification to the configured provider.
 * All calls are synchronous (intended to be called from IO dispatcher).
 * Failures are silently swallowed so they never interrupt the sync result.
 */
class NotificationDispatcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val textType = "text/plain; charset=utf-8".toMediaType()

    fun dispatch(context: Context, config: NotificationConfig, title: String, message: String) {
        if (!config.isEnabled) return
        
        val start = System.currentTimeMillis()
        var code: Int? = null
        var error: String? = null
        var requestUrl = config.displayIdentifier
        
        try {
            val request = buildRequest(config, title, message)
            if (request == null) {
                error = "Invalid configuration"
            } else {
                val response = client.newCall(request).execute()
                code = response.code
                response.close()
            }
        } catch (e: Exception) {
            error = e.message
        }
        
        val duration = System.currentTimeMillis() - start
        
        val log = WebhookLog(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = start,
            url = "[Notification] ${config.providerType.displayName}: ${requestUrl.redactSensitiveUrl()}",
            statusCode = code,
            success = error == null && (code in 200..299),
            errorMessage = error?.redactSensitiveUrl(),
            dataType = null,
            recordCount = null,
            responseTimeMs = duration,
            syncType = "notification",
            payload = "$title\n$message"
        )
        PreferencesManager(context).addWebhookLog(log)
    }

    private fun buildRequest(
        config: NotificationConfig,
        title: String,
        message: String
    ): Request? = when (config.providerType) {
        NotificationProviderType.GOTIFY      -> buildGotify(config, title, message)
        NotificationProviderType.NTFY        -> buildNtfy(config, title, message)
        NotificationProviderType.TELEGRAM    -> buildTelegram(config, title, message)
        NotificationProviderType.DISCORD     -> buildDiscord(config, title, message)
        NotificationProviderType.PUSHOVER    -> buildPushover(config, title, message)
        NotificationProviderType.CUSTOM_HTTP -> buildCustom(config, title, message)
    }

    // ── Gotify ────────────────────────────────────────────────────────────────
    private fun buildGotify(c: NotificationConfig, title: String, message: String): Request? {
        val base = c.url.trimEnd('/')
        if (base.isEmpty() || c.token.isEmpty()) return null
        val body = """{"title":${title.jsonEncode()},"message":${message.jsonEncode()},"priority":5}"""
        return Request.Builder()
            .url("$base/message")
            .addHeader("Authorization", "Bearer ${c.token}")
            .post(body.toRequestBody(jsonType))
            .build()
    }

    // ── ntfy ──────────────────────────────────────────────────────────────────
    private fun buildNtfy(c: NotificationConfig, title: String, message: String): Request? {
        val base = c.url.trimEnd('/')
        val topic = c.topic.trim()
        if (base.isEmpty() || topic.isEmpty()) return null
        return Request.Builder()
            .url("$base/$topic")
            .addHeader("Title", title)
            .addHeader("Priority", "default")
            .post(message.toRequestBody(textType))
            .build()
    }

    // ── Telegram ──────────────────────────────────────────────────────────────
    private fun buildTelegram(c: NotificationConfig, title: String, message: String): Request? {
        val token  = c.token.trim()
        val chatId = c.chatId.trim()
        if (token.isEmpty() || chatId.isEmpty()) return null
        val text   = "<b>${title.htmlEscape()}</b>\n${message.htmlEscape()}"
        val body   = """{"chat_id":${chatId.jsonEncode()},"text":${text.jsonEncode()},"parse_mode":"HTML"}"""
        return Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(body.toRequestBody(jsonType))
            .build()
    }

    // ── Discord ───────────────────────────────────────────────────────────────
    private fun buildDiscord(c: NotificationConfig, title: String, message: String): Request? {
        val url = c.url.trim()
        if (url.isEmpty()) return null
        val content = "**${title}**\\n${message}"
        val body    = """{"content":${content.jsonEncode()}}"""
        return Request.Builder()
            .url(url)
            .post(body.toRequestBody(jsonType))
            .build()
    }

    // ── Pushover ──────────────────────────────────────────────────────────────
    private fun buildPushover(c: NotificationConfig, title: String, message: String): Request? {
        val token   = c.token.trim()
        val userKey = c.userKey.trim()
        if (token.isEmpty() || userKey.isEmpty()) return null
        val body = FormBody.Builder()
            .add("token",   token)
            .add("user",    userKey)
            .add("title",   title)
            .add("message", message)
            .build()
        return Request.Builder()
            .url("https://api.pushover.net/1/messages.json")
            .post(body)
            .build()
    }

    // ── Custom HTTP ───────────────────────────────────────────────────────────
    private fun buildCustom(c: NotificationConfig, title: String, message: String): Request? {
        val url = c.url.trim()
        if (url.isEmpty()) return null
        val bodyText = if (c.bodyTemplate.isNotBlank()) {
            c.bodyTemplate
                .replace("{title}",   title)
                .replace("{message}", message)
                .replace("{status}",  title)
        } else {
            """{"title":${title.jsonEncode()},"message":${message.jsonEncode()}}"""
        }
        val builder = Request.Builder().url(url)
        c.headers.forEach { (k, v) -> builder.addHeader(k, v) }
        builder.post(bodyText.toRequestBody(jsonType))
        return builder.build()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun String.jsonEncode(): String {
        val escaped = this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun String.htmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun String.redactSensitiveUrl(): String {
        var safe = this
        safe = safe.replace(Regex("([?&](token|key|apikey|auth|api_key|secret)=)[^&]+", RegexOption.IGNORE_CASE), "$1********")
        safe = safe.replace(Regex("(discord\\.com/api/webhooks/\\d+/)[^/?&]+"), "$1********")
        safe = safe.replace(Regex("(api\\.telegram\\.org/bot)[^/]+"), "$1********")
        return safe
    }
}
