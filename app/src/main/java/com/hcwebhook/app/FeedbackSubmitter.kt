package com.hcwebhook.app

import android.content.Context
import com.feedbackjar.sdk.FeedbackResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object FeedbackSubmitter {

    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun submit(context: Context, content: String): Result<FeedbackResponse> =
        withContext(Dispatchers.IO) {
            val metadata = collectMetadata(context)
            val body = json.encodeToString(SubmitRequest(content = content, metadata = metadata))
                .toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("https://api.feedbackjar.com/widget/${HCWebhookApplication.FEEDBACKJAR_WIDGET_ID}/submit")
                .post(body)
                .build()

            try {
                http.newCall(request).execute().use { response ->
                    val raw = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Result.failure(IOException("Submit failed: HTTP ${response.code}"))
                    } else {
                        val parsed = json.decodeFromString<SubmitResponseBody>(raw)
                        Result.success(
                            FeedbackResponse(
                                postId = parsed.postId,
                                title = parsed.title,
                                type = parsed.type,
                                boardId = parsed.boardId,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun collectMetadata(context: Context): JsonObject {
        val sdkMetadata = collectSdkMetadata(context)
        val appMetadata = sdkMetadata["app"]?.jsonObject ?: buildJsonObject {}
        return buildJsonObject {
            sdkMetadata.forEach { (key, value) ->
                if (key == "app") {
                    put(key, buildJsonObject {
                        appMetadata.forEach { (k, v) -> put(k, v) }
                        put("flavor", BuildConfig.FLAVOR)
                    })
                } else {
                    put(key, value)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectSdkMetadata(context: Context): JsonObject {
        val collectorClass = Class.forName("com.feedbackjar.sdk.internal.MetadataCollector")
        val instance = collectorClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
        val method = collectorClass.getDeclaredMethod("collect", Context::class.java)
        return method.invoke(instance, context) as JsonObject
    }
}

@Serializable
private data class SubmitRequest(
    val content: String,
    val email: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val metadata: JsonObject,
)

@Serializable
private data class SubmitResponseBody(
    val success: Boolean,
    val postId: String,
    val title: String,
    val type: String,
    val boardId: String,
)
