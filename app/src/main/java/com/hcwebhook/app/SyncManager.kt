package com.hcwebhook.app

import android.content.Context
import kotlinx.coroutines.CancellationException
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

    suspend fun performSync(timeRangeDays: Int? = null, start: Instant? = null, end: Instant? = null): Result<SyncResult> = withContext(Dispatchers.IO) {
        /*
        Supports two modes:
        - timeRangeDays: the amount of days in the past to sync.
        - start/end: specific time range to sync
        Note that custom period selection may override the last sync timestamp.
        */

        try {
            val webhookConfigs = preferencesManager.getWebhookConfigs()

            if (webhookConfigs.isEmpty()) {
                return@withContext Result.failure(Exception("No webhook URLs configured"))
            }

            val enabledTypes = preferencesManager.getEnabledDataTypes()
            if (enabledTypes.isEmpty()) {
                return@withContext Result.failure(Exception("No data types enabled"))
            }

            // Keep incremental sync only for default mode.
            // Explicit ranges (start/end or timeRangeDays) always perform a full read of that window.
            val hasExplicitRange = start != null || end != null || timeRangeDays != null
            val lastSyncTimestamps = if (!hasExplicitRange) {
                enabledTypes.associateWith { type ->
                    preferencesManager.getLastSyncTimestamp(type)?.let { Instant.ofEpochMilli(it) }
                }
            } else {
                emptyMap()
            }

            // Read health data
            val healthDataResult = healthConnectManager.readHealthData(
                enabledTypes = enabledTypes,
                lastSyncTimestamps = lastSyncTimestamps,
                timeRangeDays = timeRangeDays,
                start = start,
                end = end
            )
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
                    healthData.heartRateVariability.size +
                    healthData.distance.size + healthData.activeCalories.size + healthData.totalCalories.size +
                    healthData.weight.size + healthData.height.size + healthData.bloodPressure.size +
                    healthData.bloodGlucose.size + healthData.oxygenSaturation.size + healthData.bodyTemperature.size +
                    healthData.respiratoryRate.size + healthData.restingHeartRate.size + healthData.exercise.size +
                    healthData.hydration.size + healthData.nutrition.size +
                    healthData.basalMetabolicRate.size + healthData.bodyFat.size + healthData.leanBodyMass.size +
                    healthData.vo2Max.size + healthData.boneMass.size

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isHealthDataEmpty(data: HealthData): Boolean {
        return data.steps.isEmpty() && data.sleep.isEmpty() && data.heartRate.isEmpty() &&
                data.heartRateVariability.isEmpty() &&
                data.distance.isEmpty() && data.activeCalories.isEmpty() && data.totalCalories.isEmpty() &&
                data.weight.isEmpty() && data.height.isEmpty() && data.bloodPressure.isEmpty() &&
                data.bloodGlucose.isEmpty() && data.oxygenSaturation.isEmpty() && data.bodyTemperature.isEmpty() &&
                data.respiratoryRate.isEmpty() && data.restingHeartRate.isEmpty() && data.exercise.isEmpty() &&
                data.hydration.isEmpty() && data.nutrition.isEmpty() &&
                data.basalMetabolicRate.isEmpty() && data.bodyFat.isEmpty() && data.leanBodyMass.isEmpty() &&
                data.vo2Max.isEmpty() && data.boneMass.isEmpty()
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
        if (data.heartRateVariability.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.HEART_RATE_VARIABILITY, data.heartRateVariability.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEART_RATE_VARIABILITY] = data.heartRateVariability.size
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
        if (data.basalMetabolicRate.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BASAL_METABOLIC_RATE, data.basalMetabolicRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BASAL_METABOLIC_RATE] = data.basalMetabolicRate.size
        }
        if (data.bodyFat.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BODY_FAT, data.bodyFat.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BODY_FAT] = data.bodyFat.size
        }
        if (data.leanBodyMass.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.LEAN_BODY_MASS, data.leanBodyMass.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.LEAN_BODY_MASS] = data.leanBodyMass.size
        }
        if (data.vo2Max.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.VO2_MAX, data.vo2Max.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.VO2_MAX] = data.vo2Max.size
        }
        if (data.boneMass.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.BONE_MASS, data.boneMass.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BONE_MASS] = data.boneMass.size
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
        if (data.heartRateVariability.isNotEmpty()) {
            parts.add("${data.heartRateVariability.size} HRV")
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

            if (healthData.heartRateVariability.isNotEmpty()) {
                putJsonArray("heart_rate_variability") {
                    healthData.heartRateVariability.forEach { add(buildJsonObject {
                        put("rmssd_millis", it.rmssdMillis)
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

            if (healthData.basalMetabolicRate.isNotEmpty()) {
                putJsonArray("basal_metabolic_rate") {
                    healthData.basalMetabolicRate.forEach { add(buildJsonObject {
                        put("watts", it.watts)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.bodyFat.isNotEmpty()) {
                putJsonArray("body_fat") {
                    healthData.bodyFat.forEach { add(buildJsonObject {
                        put("percentage", it.percentage)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.leanBodyMass.isNotEmpty()) {
                putJsonArray("lean_body_mass") {
                    healthData.leanBodyMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.vo2Max.isNotEmpty()) {
                putJsonArray("vo2_max") {
                    healthData.vo2Max.forEach { add(buildJsonObject {
                        put("ml_per_kg_per_min", it.mlPerKgPerMin)
                        put("time", it.time.toString())
                    }) }
                }
            }

            if (healthData.boneMass.isNotEmpty()) {
                putJsonArray("bone_mass") {
                    healthData.boneMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", it.time.toString())
                    }) }
                }
            }
        } // End of buildJsonObject block

        return json.toString()
    }
}

sealed class SyncResult {
    object NoData : SyncResult()
    data class Success(val syncCounts: Map<HealthDataType, Int>) : SyncResult()
}
