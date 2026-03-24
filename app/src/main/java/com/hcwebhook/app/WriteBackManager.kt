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
            "heart_rate" -> writeHeartRate(record.data)
            "sleep" -> writeSleep(record.data)
            "distance" -> writeDistance(record.data)
            "active_calories" -> writeActiveCalories(record.data)
            "total_calories" -> writeTotalCalories(record.data)
            "height" -> writeHeight(record.data)
            "oxygen_saturation" -> writeOxygenSaturation(record.data)
            "heart_rate_variability" -> writeHeartRateVariability(record.data)
            "basal_metabolic_rate" -> writeBasalMetabolicRate(record.data)
            "body_fat" -> writeBodyFat(record.data)
            "lean_body_mass" -> writeLeanBodyMass(record.data)
            "resting_heart_rate" -> writeRestingHeartRate(record.data)
            "vo2_max" -> writeVo2Max(record.data)
            "bone_mass" -> writeBoneMass(record.data)
            "blood_pressure" -> writeBloodPressure(record.data)
            "blood_glucose" -> writeBloodGlucose(record.data)
            "body_temperature" -> writeBodyTemperature(record.data)
            "respiratory_rate" -> writeRespiratoryRate(record.data)
            "exercise" -> writeExercise(record.data)
            "floors_climbed" -> writeFloorsClimbed(record.data)
            "menstruation" -> writeMenstruation(record.data)
            "speed" -> writeSpeed(record.data)
            "power" -> writePower(record.data)
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

    private suspend fun writeHeartRate(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertHeartRate(
            bpm = data.getLong("bpm"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeSleep(data: JSONObject): Result<Unit> {
        val stagesArray = data.optJSONArray("stages")
        val stages = if (stagesArray != null) {
            (0 until stagesArray.length()).map { i ->
                val stageObj = stagesArray.getJSONObject(i)
                androidx.health.connect.client.records.SleepSessionRecord.Stage(
                    startTime = Instant.parse(stageObj.getString("startTime")),
                    endTime = Instant.parse(stageObj.getString("endTime")),
                    stage = stageObj.getInt("stage")
                )
            }
        } else {
            emptyList()
        }
        return healthConnectManager.insertSleep(
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime")),
            stages = stages
        )
    }

    private suspend fun writeDistance(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertDistance(
            meters = data.getDouble("meters"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeActiveCalories(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertActiveCalories(
            calories = data.getDouble("calories"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeTotalCalories(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertTotalCalories(
            calories = data.getDouble("calories"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeHeight(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertHeight(
            meters = data.getDouble("meters"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeOxygenSaturation(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertOxygenSaturation(
            percentage = data.getDouble("percentage"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeHeartRateVariability(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertHeartRateVariability(
            milliseconds = data.getDouble("milliseconds"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeBasalMetabolicRate(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertBasalMetabolicRate(
            kcalPerDay = data.getDouble("kcalPerDay"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeBodyFat(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertBodyFat(
            percentage = data.getDouble("percentage"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeLeanBodyMass(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertLeanBodyMass(
            kilograms = data.getDouble("kilograms"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeRestingHeartRate(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertRestingHeartRate(
            bpm = data.getLong("bpm"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeVo2Max(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertVo2Max(
            vo2MillilitersPerMinuteKilogram = data.getDouble("vo2MillilitersPerMinuteKilogram"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeBoneMass(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertBoneMass(
            kilograms = data.getDouble("kilograms"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeBloodPressure(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertBloodPressure(
            systolic = data.getDouble("systolic"),
            diastolic = data.getDouble("diastolic"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeBloodGlucose(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertBloodGlucose(
            millimolePerLiter = data.getDouble("millimolePerLiter"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeBodyTemperature(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertBodyTemperature(
            celsius = data.getDouble("celsius"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeRespiratoryRate(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertRespiratoryRate(
            rate = data.getDouble("rate"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private suspend fun writeExercise(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertExerciseSession(
            type = data.getInt("type"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime")),
            title = if (data.has("title") && !data.isNull("title")) data.getString("title") else null
        )
    }

    private suspend fun writeFloorsClimbed(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertFloorsClimbed(
            floors = data.getDouble("floors"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeMenstruation(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertMenstruation(
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeSpeed(data: JSONObject): Result<Unit> {
        val samplesArray = data.getJSONArray("samples")
        val samples = (0 until samplesArray.length()).map { i ->
            val s = samplesArray.getJSONObject(i)
            Pair(Instant.parse(s.getString("time")), s.getDouble("metersPerSecond"))
        }
        return healthConnectManager.insertSpeed(
            samples = samples,
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writePower(data: JSONObject): Result<Unit> {
        val samplesArray = data.getJSONArray("samples")
        val samples = (0 until samplesArray.length()).map { i ->
            val s = samplesArray.getJSONObject(i)
            Pair(Instant.parse(s.getString("time")), s.getDouble("watts"))
        }
        return healthConnectManager.insertPower(
            samples = samples,
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
