package com.hcwebhook.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.Instant

class SyncManager(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val healthConnectManager = HealthConnectManager(context)

    suspend fun performSync(): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val webhookConfigs = preferencesManager.getWebhookConfigs()

            if (webhookConfigs.isEmpty()) {
                return@withContext Result.failure(Exception("No webhook URLs configured"))
            }

            val enabledTypes = preferencesManager.getEnabledDataTypes()
            if (enabledTypes.isEmpty()) {
                return@withContext Result.failure(Exception("No data types enabled"))
            }

            // Get last sync timestamps for all enabled types
            val lastSyncTimestamps = enabledTypes.associateWith { type ->
                preferencesManager.getLastSyncTimestamp(type)?.let { Instant.ofEpochMilli(it) }
            }

            // Read health data
            val healthDataResult = healthConnectManager.readHealthData(enabledTypes, lastSyncTimestamps)
            if (healthDataResult.isFailure) {
                return@withContext Result.failure(healthDataResult.exceptionOrNull() ?: Exception("Failed to read health data"))
            }

            val healthData = healthDataResult.getOrThrow()

            // Check if there's any new data
            if (isHealthDataEmpty(healthData)) {
                preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
                preferencesManager.setLastSyncSummary("No new data")
                return@withContext Result.success(SyncResult.NoData)
            }

            // Calculate total record count
            val totalRecords = healthData.steps.size + healthData.sleep.size + healthData.heartRate.size +
                    healthData.distance.size + healthData.activeCalories.size + healthData.totalCalories.size +
                    healthData.weight.size + healthData.height.size + healthData.bloodPressure.size +
                    healthData.bloodGlucose.size + healthData.oxygenSaturation.size + healthData.bodyTemperature.size +
                    healthData.respiratoryRate.size + healthData.restingHeartRate.size + healthData.exercise.size +
                    healthData.hydration.size + healthData.nutrition.size

            val webhookManager = WebhookManager(
                webhookConfigs = webhookConfigs,
                context = context,
                dataType = "all",
                recordCount = totalRecords
            )

            // Build JSON payload
            val jsonPayload = buildJsonPayload(healthData)

            // Post to webhook
            val postResult = webhookManager.postData(jsonPayload)
            if (postResult.isFailure) {
                return@withContext Result.failure(postResult.exceptionOrNull() ?: Exception("Failed to post to webhooks"))
            }

            // Update last sync timestamps
            val syncCounts = mutableMapOf<HealthDataType, Int>()
            updateSyncTimestamps(healthData, syncCounts)

            // Save last sync status for UI display
            val summary = buildSyncSummary(healthData)
            preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
            preferencesManager.setLastSyncSummary(summary)

            Result.success(SyncResult.Success(syncCounts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isHealthDataEmpty(data: HealthData): Boolean {
        return data.steps.isEmpty() && data.sleep.isEmpty() && data.heartRate.isEmpty() &&
                data.distance.isEmpty() && data.activeCalories.isEmpty() && data.totalCalories.isEmpty() &&
                data.weight.isEmpty() && data.height.isEmpty() && data.bloodPressure.isEmpty() &&
                data.bloodGlucose.isEmpty() && data.oxygenSaturation.isEmpty() && data.bodyTemperature.isEmpty() &&
                data.respiratoryRate.isEmpty() && data.restingHeartRate.isEmpty() && data.exercise.isEmpty() &&
                data.hydration.isEmpty() && data.nutrition.isEmpty()
    }

    private fun updateSyncTimestamps(data: HealthData, syncCounts: MutableMap<HealthDataType, Int>) {
        if (data.steps.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.STEPS, data.steps.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.STEPS] = data.steps.size
        }
        if (data.sleep.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.SLEEP, data.sleep.maxOf { it.sessionEndTime }.toEpochMilli())
            syncCounts[HealthDataType.SLEEP] = data.sleep.size
        }
        if (data.heartRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HEART_RATE, data.heartRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEART_RATE] = data.heartRate.size
        }
        if (data.distance.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.DISTANCE, data.distance.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.DISTANCE] = data.distance.size
        }
        if (data.activeCalories.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.ACTIVE_CALORIES, data.activeCalories.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.ACTIVE_CALORIES] = data.activeCalories.size
        }
        if (data.totalCalories.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.TOTAL_CALORIES, data.totalCalories.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.TOTAL_CALORIES] = data.totalCalories.size
        }
        if (data.weight.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.WEIGHT, data.weight.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.WEIGHT] = data.weight.size
        }
        if (data.height.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HEIGHT, data.height.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEIGHT] = data.height.size
        }
        if (data.bloodPressure.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BLOOD_PRESSURE, data.bloodPressure.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BLOOD_PRESSURE] = data.bloodPressure.size
        }
        if (data.bloodGlucose.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BLOOD_GLUCOSE, data.bloodGlucose.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BLOOD_GLUCOSE] = data.bloodGlucose.size
        }
        if (data.oxygenSaturation.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.OXYGEN_SATURATION, data.oxygenSaturation.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.OXYGEN_SATURATION] = data.oxygenSaturation.size
        }
        if (data.bodyTemperature.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BODY_TEMPERATURE, data.bodyTemperature.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BODY_TEMPERATURE] = data.bodyTemperature.size
        }
        if (data.respiratoryRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.RESPIRATORY_RATE, data.respiratoryRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.RESPIRATORY_RATE] = data.respiratoryRate.size
        }
        if (data.restingHeartRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.RESTING_HEART_RATE, data.restingHeartRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.RESTING_HEART_RATE] = data.restingHeartRate.size
        }
        if (data.exercise.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.EXERCISE, data.exercise.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.EXERCISE] = data.exercise.size
        }
        if (data.hydration.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HYDRATION, data.hydration.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.HYDRATION] = data.hydration.size
        }
        if (data.nutrition.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.NUTRITION, data.nutrition.maxOf { it.endTime }.toEpochMilli())
            syncCounts[HealthDataType.NUTRITION] = data.nutrition.size
        }
    }

    private fun buildSyncSummary(data: HealthData): String {
        val parts = mutableListOf<String>()

        if (data.steps.isNotEmpty()) {
            val total = data.steps.sumOf { it.count }
            parts.add("%,d steps".format(total))
        }
        if (data.distance.isNotEmpty()) {
            val totalKm = data.distance.sumOf { it.meters } / 1000.0
            parts.add("%.1f km".format(totalKm))
        }
        if (data.activeCalories.isNotEmpty()) {
            val total = data.activeCalories.sumOf { it.calories }.toInt()
            parts.add("$total cal")
        }
        if (data.sleep.isNotEmpty()) {
            parts.add("${data.sleep.size} sleep")
        }
        if (data.exercise.isNotEmpty()) {
            parts.add("${data.exercise.size} exercise")
        }
        if (data.weight.isNotEmpty()) {
            parts.add("${data.weight.size} weight")
        }
        if (data.heartRate.isNotEmpty()) {
            parts.add("${data.heartRate.size} HR")
        }

        return if (parts.isEmpty()) "No new data" else parts.joinToString(" · ")
    }

    private fun buildJsonPayload(healthData: HealthData): String {
        val json = buildJsonObject {
            put("timestamp", Instant.now().toString())
            put("app_version", "1.0")

            if (healthData.steps.isNotEmpty()) {
                putJsonArray("steps") {
                    healthData.steps.forEach { step ->
                        add(buildJsonObject {
                            put("count", step.count)
                            put("start_time", step.startTime.toString())
                            put("end_time", step.endTime.toString())
                        })
                    }
                }
            }

            if (healthData.sleep.isNotEmpty()) {
                putJsonArray("sleep") {
                    healthData.sleep.forEach { sleep ->
                        add(buildJsonObject {
                            put("session_end_time", sleep.sessionEndTime.toString())
                            put("duration_seconds", sleep.duration.seconds)
                            putJsonArray("stages") {
                                sleep.stages.forEach { stage ->
                                    add(buildJsonObject {
                                        put("stage", stage.stage)
                                        put("start_time", stage.startTime.toString())
                                        put("end_time", stage.endTime.toString())
                                        put("duration_seconds", stage.duration.seconds)
                                    })
                                }
                            }
                        })
                    }
                }
            }

            if (healthData.heartRate.isNotEmpty()) {
                putJsonArray("heart_rate") {
                    healthData.heartRate.forEach { add(buildJsonObject {
                        put("bpm", it.bpm)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.distance.isNotEmpty()) {
                putJsonArray("distance") {
                    healthData.distance.forEach { add(buildJsonObject {
                        put("meters", it.meters)
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                    }) }
                }
            }

            if (healthData.activeCalories.isNotEmpty()) {
                putJsonArray("active_calories") {
                    healthData.activeCalories.forEach { add(buildJsonObject {
                        put("calories", it.calories)
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                    }) }
                }
            }

            if (healthData.totalCalories.isNotEmpty()) {
                putJsonArray("total_calories") {
                    healthData.totalCalories.forEach { add(buildJsonObject {
                        put("calories", it.calories)
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                    }) }
                }
            }

            if (healthData.weight.isNotEmpty()) {
                putJsonArray("weight") {
                    healthData.weight.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.height.isNotEmpty()) {
                putJsonArray("height") {
                    healthData.height.forEach { add(buildJsonObject {
                        put("meters", it.meters)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.bloodPressure.isNotEmpty()) {
                putJsonArray("blood_pressure") {
                    healthData.bloodPressure.forEach { add(buildJsonObject {
                        put("systolic", it.systolic)
                        put("diastolic", it.diastolic)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.bloodGlucose.isNotEmpty()) {
                putJsonArray("blood_glucose") {
                    healthData.bloodGlucose.forEach { add(buildJsonObject {
                        put("mmol_per_liter", it.mmolPerLiter)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.oxygenSaturation.isNotEmpty()) {
                putJsonArray("oxygen_saturation") {
                    healthData.oxygenSaturation.forEach { add(buildJsonObject {
                        put("percentage", it.percentage)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.bodyTemperature.isNotEmpty()) {
                putJsonArray("body_temperature") {
                    healthData.bodyTemperature.forEach { add(buildJsonObject {
                        put("celsius", it.celsius)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.respiratoryRate.isNotEmpty()) {
                putJsonArray("respiratory_rate") {
                    healthData.respiratoryRate.forEach { add(buildJsonObject {
                        put("rate", it.rate)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.restingHeartRate.isNotEmpty()) {
                putJsonArray("resting_heart_rate") {
                    healthData.restingHeartRate.forEach { add(buildJsonObject {
                        put("bpm", it.bpm)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.exercise.isNotEmpty()) {
                putJsonArray("exercise") {
                    healthData.exercise.forEach { add(buildJsonObject {
                        put("type", it.type)
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                        put("duration_seconds", it.duration.seconds)
                    }) }
                }
            }

            if (healthData.hydration.isNotEmpty()) {
                putJsonArray("hydration") {
                    healthData.hydration.forEach { add(buildJsonObject {
                        put("liters", it.liters)
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                    }) }
                }
            }

            if (healthData.nutrition.isNotEmpty()) {
                putJsonArray("nutrition") {
                    healthData.nutrition.forEach { add(buildJsonObject {
                        it.calories?.let { cal -> put("calories", cal) }
                        it.protein?.let { prot -> put("protein_grams", prot) }
                        it.carbs?.let { carb -> put("carbs_grams", carb) }
                        it.fat?.let { f -> put("fat_grams", f) }
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                    }) }
                }
            }
        }

        return json.toString()
    }
}

sealed class SyncResult {
    object NoData : SyncResult()
    data class Success(val syncCounts: Map<HealthDataType, Int>) : SyncResult()
}