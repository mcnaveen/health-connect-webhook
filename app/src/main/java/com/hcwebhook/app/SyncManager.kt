package com.hcwebhook.app

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant

/** Emit a record's Health Connect provenance as a nested "metadata" object. */
private fun JsonObjectBuilder.putRecordMetadata(meta: RecordMetadata) {
    putJsonObject("metadata") {
        put("data_origin", meta.dataOrigin)
        put("recording_method", meta.recordingMethod)
        if (meta.deviceManufacturer != null || meta.deviceModel != null || meta.deviceType != null) {
            putJsonObject("device") {
                meta.deviceManufacturer?.let { put("manufacturer", it) }
                meta.deviceModel?.let { put("model", it) }
                meta.deviceType?.let { put("type", it) }
            }
        }
    }
}

class SyncManager(private val context: Context) {

    private val appVersionName: String by lazy {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private val preferencesManager = PreferencesManager(context)
    private val healthConnectManager = HealthConnectManager(context)

    suspend fun getRealtimeJsonPayload(
        timeRangeDays: Int? = null,
        start: Instant? = null,
        end: Instant? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val enabledTypes = preferencesManager.getEnabledDataTypes()
            if (enabledTypes.isEmpty()) {
                return@withContext Result.failure(Exception("No data types enabled"))
            }

            // Fresh read: do not use last sync timestamps
            val healthDataResult = healthConnectManager.readHealthData(
                enabledTypes = enabledTypes,
                lastSyncTimestamps = emptyMap(),
                timeRangeDays = timeRangeDays,
                start = start,
                end = end,
                dataTypeResolutions = preferencesManager.getDataTypeResolutions(),
            )
            if (healthDataResult.isFailure) {
                return@withContext Result.failure(
                    healthDataResult.exceptionOrNull() ?: Exception("Failed to read health data")
                )
            }

            val jsonPayload = buildJsonPayload(healthDataResult.getOrThrow())
            LocalHttpServerManager.publishPayload(jsonPayload)
            Result.success(jsonPayload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performSync(timeRangeDays: Int? = null, start: Instant? = null, end: Instant? = null, syncType: String = "auto", targetWebhooks: List<WebhookConfig>? = null): Result<SyncResult> = withContext(Dispatchers.IO) {
        /*
        Supports two modes:
        - timeRangeDays: the amount of days in the past to sync.
        - start/end: specific time range to sync
        Note that custom period selection may override the last sync timestamp.
        */

        try {
            val webhookConfigs = preferencesManager.getWebhookConfigs()
            val enabledWebhookConfigs = (targetWebhooks ?: webhookConfigs).filter { it.isEnabled }
            val localTcpEnabled = preferencesManager.isLocalTcpEnabled()

            if (enabledWebhookConfigs.isEmpty() && !localTcpEnabled) {
                return@withContext Result.failure(Exception("No enabled webhook URLs configured and local TCP server is disabled"))
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
                end = end,
                dataTypeResolutions = preferencesManager.getDataTypeResolutions(),
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

            // Build full payload (also used by local TCP server)
            val fullPayload = buildJsonPayload(healthData)
            LocalHttpServerManager.publishPayload(fullPayload)

            // Post to each enabled webhook with optional per-webhook data type filtering
            if (enabledWebhookConfigs.isNotEmpty()) {
                var atLeastOneSuccess = false
                var atLeastOneAttempted = false
                var lastFailure: Throwable? = null

                val dispatcher = NotificationDispatcher()
                val globalNotifs = preferencesManager.getNotificationConfigs()
                val aggregatedNotifs = mutableMapOf<NotificationConfig, MutableList<String>>()

                for (config in enabledWebhookConfigs) {
                    val filteredData = if (config.dataTypeFilter != null) {
                        filterHealthData(healthData, config.dataTypeFilter)
                    } else {
                        healthData
                    }
                    if (isHealthDataEmpty(filteredData)) continue
                    atLeastOneAttempted = true
                    val payload = if (config.dataTypeFilter != null) buildJsonPayload(filteredData) else fullPayload
                    val totalRecords = countHealthData(filteredData)

                    val manager = WebhookManager(
                        webhookConfigs = listOf(config),
                        context = context,
                        dataType = "all",
                        recordCount = totalRecords,
                        syncType = syncType,
                        payload = payload
                    )
                    val result = manager.postData(payload)
                    
                    val notifConfigs = config.notificationConfigIds.mapNotNull { id -> 
                        globalNotifs.find { it.id == id } 
                    }

                    if (result.isSuccess) {
                        atLeastOneSuccess = true
                        val msg = "✅ ${config.url}: $totalRecords records"
                        notifConfigs.forEach { nc ->
                            aggregatedNotifs.getOrPut(nc) { mutableListOf() }.add(msg)
                        }
                    } else {
                        lastFailure = result.exceptionOrNull()
                        val msg = "❌ ${config.url}: ${lastFailure?.message ?: "Error"}"
                        notifConfigs.forEach { nc ->
                            aggregatedNotifs.getOrPut(nc) { mutableListOf() }.add(msg)
                        }
                    }
                }

                aggregatedNotifs.forEach { (nc, messages) ->
                    val title = if (messages.any { it.startsWith("❌") }) "Sync Completed with Errors" else "Sync Succeeded"
                    dispatcher.dispatch(
                        context = context,
                        config = nc,
                        title = title,
                        message = messages.joinToString("\n")
                    )
                }

                if (!atLeastOneAttempted) {
                    preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
                    preferencesManager.setLastSyncSummary("No matching data")
                    return@withContext Result.success(SyncResult.NoMatchingData)
                }
                if (!atLeastOneSuccess) {
                    return@withContext Result.failure(lastFailure ?: Exception("Failed to post to webhooks"))
                }
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

    private fun filterHealthData(data: HealthData, allowedTypes: Set<String>): HealthData {
        val allowed = allowedTypes.map { it.uppercase() }.toSet()
        return data.copy(
            steps = if ("STEPS" in allowed) data.steps else emptyList(),
            sleep = if ("SLEEP" in allowed) data.sleep else emptyList(),
            heartRate = if ("HEART_RATE" in allowed) data.heartRate else emptyList(),
            heartRateVariability = if ("HEART_RATE_VARIABILITY" in allowed) data.heartRateVariability else emptyList(),
            distance = if ("DISTANCE" in allowed) data.distance else emptyList(),
            activeCalories = if ("ACTIVE_CALORIES" in allowed) data.activeCalories else emptyList(),
            totalCalories = if ("TOTAL_CALORIES" in allowed) data.totalCalories else emptyList(),
            weight = if ("WEIGHT" in allowed) data.weight else emptyList(),
            height = if ("HEIGHT" in allowed) data.height else emptyList(),
            bloodPressure = if ("BLOOD_PRESSURE" in allowed) data.bloodPressure else emptyList(),
            bloodGlucose = if ("BLOOD_GLUCOSE" in allowed) data.bloodGlucose else emptyList(),
            oxygenSaturation = if ("OXYGEN_SATURATION" in allowed) data.oxygenSaturation else emptyList(),
            bodyTemperature = if ("BODY_TEMPERATURE" in allowed) data.bodyTemperature else emptyList(),
            skinTemperature = if ("SKIN_TEMPERATURE" in allowed) data.skinTemperature else emptyList(),
            respiratoryRate = if ("RESPIRATORY_RATE" in allowed) data.respiratoryRate else emptyList(),
            restingHeartRate = if ("RESTING_HEART_RATE" in allowed) data.restingHeartRate else emptyList(),
            exercise = if ("EXERCISE" in allowed) data.exercise else emptyList(),
            hydration = if ("HYDRATION" in allowed) data.hydration else emptyList(),
            nutrition = if ("NUTRITION" in allowed) data.nutrition else emptyList(),
            basalMetabolicRate = if ("BASAL_METABOLIC_RATE" in allowed) data.basalMetabolicRate else emptyList(),
            bodyFat = if ("BODY_FAT" in allowed) data.bodyFat else emptyList(),
            leanBodyMass = if ("LEAN_BODY_MASS" in allowed) data.leanBodyMass else emptyList(),
            vo2Max = if ("VO2_MAX" in allowed) data.vo2Max else emptyList(),
            boneMass = if ("BONE_MASS" in allowed) data.boneMass else emptyList()
        )
    }

    private fun countHealthData(data: HealthData): Int {
        return data.steps.size + data.sleep.size + data.heartRate.size +
                data.heartRateVariability.size + data.distance.size + data.activeCalories.size +
                data.totalCalories.size + data.weight.size + data.height.size +
                data.bloodPressure.size + data.bloodGlucose.size + data.oxygenSaturation.size +
                data.bodyTemperature.size + data.skinTemperature.size + data.respiratoryRate.size +
                data.restingHeartRate.size + data.exercise.size + data.hydration.size +
                data.nutrition.size + data.basalMetabolicRate.size + data.bodyFat.size +
                data.leanBodyMass.size + data.vo2Max.size + data.boneMass.size
    }

    private fun isHealthDataEmpty(data: HealthData): Boolean {
        return data.steps.isEmpty() && data.sleep.isEmpty() && data.heartRate.isEmpty() &&
                data.heartRateVariability.isEmpty() &&
                data.distance.isEmpty() && data.activeCalories.isEmpty() && data.totalCalories.isEmpty() &&
                data.weight.isEmpty() && data.height.isEmpty() && data.bloodPressure.isEmpty() &&
                data.bloodGlucose.isEmpty() && data.oxygenSaturation.isEmpty() && data.bodyTemperature.isEmpty() &&
                data.skinTemperature.isEmpty() &&
                data.respiratoryRate.isEmpty() && data.restingHeartRate.isEmpty() && data.exercise.isEmpty() &&
                data.hydration.isEmpty() && data.nutrition.isEmpty() &&
                data.basalMetabolicRate.isEmpty() && data.bodyFat.isEmpty() && data.leanBodyMass.isEmpty() &&
                data.vo2Max.isEmpty() && data.boneMass.isEmpty()
    }

    /**
     * Returns the epoch-millisecond value of the latest endTime that is NOT
     * in the future, or null if every endTime is in the future.
     *
     * Clamping to now() prevents future-dated projection records (e.g., Google
     * Health's daily calorie/distance projection ending at local midnight) from
     * advancing the per-type lastSync cursor past wall-clock time. Without this
     * clamp, all genuinely-closed records produced after the first sync are
     * filtered out by the endTime >= lastSync guard in each readXxx function.
     */
    private fun clampedMaxEndMs(endTimes: Sequence<Instant>, now: Instant): Long? =
        endTimes.filter { !it.isAfter(now) }.maxOrNull()?.toEpochMilli()

    private fun updateSyncTimestamps(data: HealthData, syncCounts: MutableMap<HealthDataType, Int>) {
        val now = Instant.now()
        if (data.steps.isNotEmpty()) {
            clampedMaxEndMs(data.steps.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.STEPS, it) }
            syncCounts[HealthDataType.STEPS] = data.steps.size
        }
        if (data.sleep.isNotEmpty()) {
            clampedMaxEndMs(data.sleep.asSequence().map { it.sessionEndTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.SLEEP, it) }
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
            clampedMaxEndMs(data.distance.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.DISTANCE, it) }
            syncCounts[HealthDataType.DISTANCE] = data.distance.size
        }
        if (data.activeCalories.isNotEmpty()) {
            clampedMaxEndMs(data.activeCalories.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.ACTIVE_CALORIES, it) }
            syncCounts[HealthDataType.ACTIVE_CALORIES] = data.activeCalories.size
        }
        if (data.totalCalories.isNotEmpty()) {
            // Clamp to now() so a future-dated endTime (e.g., daily projection
            // from Google Health ending at local midnight) cannot advance the
            // cursor past wall-clock time and starve subsequent closed records.
            clampedMaxEndMs(data.totalCalories.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.TOTAL_CALORIES, it) }
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
        if (data.skinTemperature.isNotEmpty()) {
            preferencesManager.setLastSyncTimestamp(HealthDataType.SKIN_TEMPERATURE, data.skinTemperature.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.SKIN_TEMPERATURE] = data.skinTemperature.size
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
            clampedMaxEndMs(data.exercise.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.EXERCISE, it) }
            syncCounts[HealthDataType.EXERCISE] = data.exercise.size
        }
        if (data.hydration.isNotEmpty()) {
            clampedMaxEndMs(data.hydration.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.HYDRATION, it) }
            syncCounts[HealthDataType.HYDRATION] = data.hydration.size
        }
        if (data.nutrition.isNotEmpty()) {
            clampedMaxEndMs(data.nutrition.asSequence().map { it.endTime }, now)
                ?.let { preferencesManager.setLastSyncTimestamp(HealthDataType.NUTRITION, it) }
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
            put("app_version", appVersionName)

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
                            sleep.metadata?.let { meta -> putRecordMetadata(meta) }
                        })
                    }
                }
            }

            if (healthData.heartRate.isNotEmpty()) {
                putJsonArray("heart_rate") {
                    healthData.heartRate.forEach { hr -> add(buildJsonObject {
                        if (hr.min != null) {
                            put("time", hr.time.toString())
                            put("avg", hr.bpm)
                            put("min", hr.min)
                            put("max", hr.max)
                        } else {
                            put("bpm", hr.bpm)
                            put("time", hr.time.toString())
                        }
                        hr.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.heartRateVariability.isNotEmpty()) {
                putJsonArray("heart_rate_variability") {
                    healthData.heartRateVariability.forEach { hrv -> add(buildJsonObject {
                        if (hrv.min != null) {
                            put("time", hrv.time.toString())
                            put("avg", hrv.rmssdMillis)
                            put("min", hrv.min)
                            put("max", hrv.max)
                        } else {
                            put("rmssd_millis", hrv.rmssdMillis)
                            put("time", hrv.time.toString())
                        }
                        hrv.metadata?.let { meta -> putRecordMetadata(meta) }
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
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.weight.isNotEmpty()) {
                putJsonArray("weight") {
                    healthData.weight.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.height.isNotEmpty()) {
                putJsonArray("height") {
                    healthData.height.forEach { add(buildJsonObject {
                        put("meters", it.meters)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.bloodPressure.isNotEmpty()) {
                putJsonArray("blood_pressure") {
                    healthData.bloodPressure.forEach { add(buildJsonObject {
                        put("systolic", it.systolic)
                        put("diastolic", it.diastolic)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.bloodGlucose.isNotEmpty()) {
                putJsonArray("blood_glucose") {
                    healthData.bloodGlucose.forEach { add(buildJsonObject {
                        put("mmol_per_liter", it.mmolPerLiter)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.oxygenSaturation.isNotEmpty()) {
                putJsonArray("oxygen_saturation") {
                    healthData.oxygenSaturation.forEach { o2 -> add(buildJsonObject {
                        if (o2.min != null) {
                            put("time", o2.time.toString())
                            put("avg", o2.percentage)
                            put("min", o2.min)
                            put("max", o2.max)
                        } else {
                            put("percentage", o2.percentage)
                            put("time", o2.time.toString())
                        }
                        o2.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.bodyTemperature.isNotEmpty()) {
                putJsonArray("body_temperature") {
                    healthData.bodyTemperature.forEach { add(buildJsonObject {
                        put("celsius", it.celsius)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.skinTemperature.isNotEmpty()) {
                putJsonArray("skin_temperature") {
                    healthData.skinTemperature.forEach { skin -> add(buildJsonObject {
                        put("time", skin.time.toString())
                        if (skin.minDeltaCelsius != null) {
                            put("avg_delta_celsius", skin.deltaCelsius)
                            put("min_delta_celsius", skin.minDeltaCelsius)
                            put("max_delta_celsius", skin.maxDeltaCelsius)
                        } else {
                            put("delta_celsius", skin.deltaCelsius)
                        }
                        skin.baselineCelsius?.let { baseline -> put("baseline_celsius", baseline) }
                        put("measurement_location", skin.measurementLocation)
                        skin.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.respiratoryRate.isNotEmpty()) {
                putJsonArray("respiratory_rate") {
                    healthData.respiratoryRate.forEach { resp -> add(buildJsonObject {
                        if (resp.min != null) {
                            put("time", resp.time.toString())
                            put("avg", resp.rate)
                            put("min", resp.min)
                            put("max", resp.max)
                        } else {
                            put("rate", resp.rate)
                            put("time", resp.time.toString())
                        }
                        resp.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.restingHeartRate.isNotEmpty()) {
                putJsonArray("resting_heart_rate") {
                    healthData.restingHeartRate.forEach { add(buildJsonObject {
                        put("bpm", it.bpm)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
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
                        it.distanceMeters?.let { distanceMeters ->
                            put("distance_meters", distanceMeters)
                        }
                        it.steps?.let { steps ->
                            put("steps", steps)
                        }
                        it.avgCadenceSpm?.let { avgCadenceSpm ->
                            put("avg_cadence_spm", avgCadenceSpm)
                        }
                        it.maxCadenceSpm?.let { maxCadenceSpm ->
                            put("max_cadence_spm", maxCadenceSpm)
                        }
                        it.strideLengthMeters?.let { strideLengthMeters ->
                            put("stride_length_m", strideLengthMeters)
                        }
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.hydration.isNotEmpty()) {
                putJsonArray("hydration") {
                    healthData.hydration.forEach { add(buildJsonObject {
                        put("liters", it.liters)
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
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
                        it.sugar?.let { sugar -> put("sugar_grams", sugar) }
                        it.sodium?.let { sodium -> put("sodium_grams", sodium) }
                        it.dietaryFiber?.let { fiber -> put("dietary_fiber_grams", fiber) }
                        it.name?.let { name -> put("name", name) }
                        put("start_time", it.startTime.toString())
                        put("end_time", it.endTime.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.basalMetabolicRate.isNotEmpty()) {
                putJsonArray("basal_metabolic_rate") {
                    healthData.basalMetabolicRate.forEach { add(buildJsonObject {
                        put("watts", it.watts)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.bodyFat.isNotEmpty()) {
                putJsonArray("body_fat") {
                    healthData.bodyFat.forEach { add(buildJsonObject {
                        put("percentage", it.percentage)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.leanBodyMass.isNotEmpty()) {
                putJsonArray("lean_body_mass") {
                    healthData.leanBodyMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.vo2Max.isNotEmpty()) {
                putJsonArray("vo2_max") {
                    healthData.vo2Max.forEach { add(buildJsonObject {
                        put("ml_per_kg_per_min", it.mlPerKgPerMin)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }

            if (healthData.boneMass.isNotEmpty()) {
                putJsonArray("bone_mass") {
                    healthData.boneMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", it.time.toString())
                        it.metadata?.let { meta -> putRecordMetadata(meta) }
                    }) }
                }
            }
        } // End of buildJsonObject block

        return json.toString()
    }
}

sealed class SyncResult {
    object NoData : SyncResult()
    object NoMatchingData : SyncResult()
    data class Success(val syncCounts: Map<HealthDataType, Int>) : SyncResult()
}
