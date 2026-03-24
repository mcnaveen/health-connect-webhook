package com.hcwebhook.app

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

class WriteBackManager(private val context: Context) {

    private val healthConnectManager = HealthConnectManager(context)
    private val preferencesManager = PreferencesManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processPendingWrites(): Result<Int> {
        if (!preferencesManager.isWriteBackEnabled()) {
            return Result.success(0)
        }

        val webhookConfigs = preferencesManager.getWebhookConfigs()
        if (webhookConfigs.isEmpty()) {
            return Result.success(0)
        }

        val config = webhookConfigs.first()
        val baseUrl = config.url.trimEnd('/')

        val pendingRecords = try {
            fetchPendingWrites(baseUrl, config.headers)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch pending writes", e)
            return Result.failure(e)
        }

        if (pendingRecords.isEmpty()) {
            return Result.success(0)
        }

        var successCount = 0
        for (record in pendingRecords) {
            val writeResult = writeRecord(record)
            if (writeResult.isSuccess) {
                val confirmed = confirmWrite(baseUrl, config.headers, record.id)
                if (confirmed) {
                    successCount++
                } else {
                    Log.w(TAG, "Write succeeded but confirm failed for record ${record.id}")
                    successCount++
                }
            } else {
                Log.w(TAG, "Failed to write record ${record.id}: ${writeResult.exceptionOrNull()?.message}")
            }
        }

        return Result.success(successCount)
    }

    private fun fetchPendingWrites(baseUrl: String, headers: Map<String, String>): List<PendingWriteRecord> {
        val requestBuilder = Request.Builder()
            .url("$baseUrl/pending-writes")
            .get()
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body?.string() ?: return emptyList()
        if (body.isBlank()) return emptyList()

        val root = JSONObject(body)
        val jsonArray = root.optJSONArray("pending") ?: return emptyList()
        val records = mutableListOf<PendingWriteRecord>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            records.add(PendingWriteRecord(
                id = obj.getString("id"),
                type = obj.getString("type"),
                data = obj.getJSONObject("data")
            ))
        }
        return records
    }

    private suspend fun writeRecord(record: PendingWriteRecord): Result<Unit> {
        return when (record.type) {
            "nutrition" -> writeNutrition(record.data)
            "hydration" -> writeHydration(record.data)
            "weight" -> writeWeight(record.data)
            "steps" -> writeSteps(record.data)
            else -> Result.failure(Exception("Unsupported write type: ${record.type}"))
        }
    }

    private suspend fun writeNutrition(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertNutrition(
            calories = data.optDoubleOrNull("calories"),
            protein = data.optDoubleOrNull("protein"),
            carbs = data.optDoubleOrNull("carbs"),
            fat = data.optDoubleOrNull("fat"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeHydration(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertHydration(
            liters = data.getDouble("liters"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeWeight(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertWeight(
            kilograms = data.getDouble("kilograms"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeSteps(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertSteps(
            count = data.getLong("count"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private fun confirmWrite(baseUrl: String, headers: Map<String, String>, recordId: String): Boolean {
        return try {
            val payload = JSONObject().put("id", recordId).toString()
            val requestBuilder = Request.Builder()
                .url("$baseUrl/confirm-write")
                .post(payload.toRequestBody(jsonMediaType))
            headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error confirming write for $recordId", e)
            false
        }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        return if (has(key) && !isNull(key)) getDouble(key) else null
    }

    private data class PendingWriteRecord(
        val id: String,
        val type: String,
        val data: JSONObject
    )

    companion object {
        private const val TAG = "WriteBackManager"
        private const val TIMEOUT_SECONDS = 30L
    }
}
