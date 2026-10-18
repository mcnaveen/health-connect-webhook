package com.hcwebhook.app

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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
private fun JsonObjectBuilder.putRecordMetadata(meta: RecordMetadata, time: PayloadTime) {
    putJsonObject("metadata") {
        put("data_origin", meta.dataOrigin)
        put("recording_method", meta.recordingMethod)
        // Record identity, so a receiver can replace an edited record and apply "deleted".
        if (meta.id.isNotEmpty()) put("id", meta.id)
        meta.clientRecordId?.let { put("client_record_id", it) }
        if (meta.clientRecordVersion != 0L) put("client_record_version", meta.clientRecordVersion)
        meta.lastModifiedTime?.let { put("last_modified_time", time.format(it)) }
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

            val jsonPayload = try {
                buildJsonPayload(healthDataResult.getOrThrow())
            } catch (oom: OutOfMemoryError) {
                return@withContext Result.failure(
                    Exception(
                        "Out of memory while building JSON. Raise sample resolution or use a shorter range.",
                        oom
                    )
                )
            }
            LocalHttpServerManager.publishPayload(jsonPayload)
            Result.success(jsonPayload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } catch (e: OutOfMemoryError) {
            Result.failure(
                Exception("Out of memory while reading health data. Raise sample resolution or use a shorter range.", e)
            )
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

            // Incremental sync also asks the Health Connect Changes API what changed since the
            // last sync: edits and late writes that the lastSync filter drops, and deletions.
            val readEnd = end ?: Instant.now()
            var typeChanges = if (!hasExplicitRange) {
                healthConnectManager.readChanges(
                    types = enabledTypes,
                    tokens = enabledTypes.associateWith { preferencesManager.getChangesToken(it) },
                    lastSyncTimestamps = lastSyncTimestamps,
                    windowEnd = readEnd,
                )
            } else {
                emptyMap()
            }

            // Read health data
            suspend fun readData(changes: Map<HealthDataType, TypeChanges>): Result<HealthData> =
                healthConnectManager.readHealthData(
                    enabledTypes = enabledTypes,
                    lastSyncTimestamps = lastSyncTimestamps,
                    timeRangeDays = timeRangeDays,
                    start = start,
                    end = readEnd,
                    dataTypeResolutions = preferencesManager.getDataTypeResolutions(),
                    changes = changes.mapValues { it.value.changed },
                ).map { data ->
                    data.copy(
                        deleted = changes.flatMap { (type, typeChange) ->
                            typeChange.deletedIds.map { DeletedRecord(type, it) }
                        },
                    )
                }

            var healthDataResult = readData(typeChanges)
            if (healthDataResult.isFailure) {
                return@withContext Result.failure(healthDataResult.exceptionOrNull() ?: Exception("Failed to read health data"))
            }
            var healthData = healthDataResult.getOrThrow()

            // A big late write (e.g. weeks of heart rate from a watch) can make the changed
            // records too large to send. The tokens move on only after a successful sync, so
            // every sync would fail again. Drop the changed records once and send the rest.
            if (countHealthData(healthData) > MAX_RECORDS_PER_PAYLOAD && typeChanges.hasChangedRecords()) {
                Log.w(TAG, "Payload too large with changed records; sending without them")
                typeChanges = typeChanges.withoutChangedRecords()
                healthDataResult = readData(typeChanges)
                if (healthDataResult.isFailure) {
                    return@withContext Result.failure(healthDataResult.exceptionOrNull() ?: Exception("Failed to read health data"))
                }
                healthData = healthDataResult.getOrThrow()
            }

            // Check if there's any new data
            if (isHealthDataEmpty(healthData)) {
                saveChangesTokens(typeChanges)
                preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
                preferencesManager.setLastSyncSummary("No new data")
                return@withContext Result.success(SyncResult.NoData)
            }

            val recordCount = countHealthData(healthData)
            if (recordCount > MAX_RECORDS_PER_PAYLOAD) {
                return@withContext Result.failure(
                    Exception(
                        "Payload too large ($recordCount records). " +
                            "Raise sample resolution (e.g. heart rate 1 min) or sync a shorter range."
                    )
                )
            }

            // Build full payload (also used by local TCP server)
            val fullPayload = try {
                buildJsonPayload(healthData)
            } catch (oom: OutOfMemoryError) {
                return@withContext Result.failure(
                    Exception(
                        "Out of memory while building JSON ($recordCount records). " +
                            "Raise sample resolution or sync a shorter range.",
                        oom
                    )
                )
            }
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
                    val totalRecords = countHealthData(filteredData)

                    val result = when (config.deliveryFormat) {
                        WebhookDeliveryFormat.JSON -> {
                            val payload = try {
                                if (config.dataTypeFilter != null) buildJsonPayload(filteredData) else fullPayload
                            } catch (oom: OutOfMemoryError) {
                                lastFailure = Exception(
                                    "Out of memory while building JSON for ${config.url}. Raise sample resolution.",
                                    oom
                                )
                                continue
                            }
                            val manager = WebhookManager(
                                webhookConfigs = listOf(config),
                                context = context,
                                dataType = "all",
                                recordCount = totalRecords,
                                syncType = syncType,
                                payload = payload
                            )
                            manager.postData(payload)
                        }
                        WebhookDeliveryFormat.GRPC -> {
                            val grpcPayload = try {
                                ProtobufPayloadBuilder.build(filteredData, appVersionName)
                            } catch (oom: OutOfMemoryError) {
                                lastFailure = Exception(
                                    "Out of memory while building protobuf for ${config.url}. Raise sample resolution.",
                                    oom
                                )
                                continue
                            }
                            val logJson = try {
                                if (config.dataTypeFilter != null) buildJsonPayload(filteredData) else fullPayload
                            } catch (_: OutOfMemoryError) {
                                null
                            }
                            GrpcWebhookClient.deliver(
                                config = config,
                                payload = grpcPayload,
                                context = context,
                                dataType = "all",
                                recordCount = totalRecords,
                                syncType = syncType,
                                logPayload = logJson
                            )
                        }
                    }
                    
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
            updateSyncTimestamps(healthData, syncCounts, moveTimestamps = !hasExplicitRange)
            saveChangesTokens(typeChanges)

            // Save last sync status for UI display
            val summary = buildSyncSummary(healthData)
            preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
            preferencesManager.setLastSyncSummary(summary)

            Result.success(SyncResult.Success(syncCounts))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } catch (e: OutOfMemoryError) {
            Result.failure(
                Exception(
                    "Out of memory during sync. Raise sample resolution (e.g. heart rate 1 min) or sync a shorter range.",
                    e
                )
            )
        }
    }

    /** Store the next Changes tokens only after a sync succeeds, so failed syncs read the changes again. */
    private fun saveChangesTokens(typeChanges: Map<HealthDataType, TypeChanges>) {
        typeChanges.forEach { (type, changes) -> preferencesManager.setChangesToken(type, changes.nextToken) }
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
            bodyWaterMass = if ("BODY_WATER_MASS" in allowed) data.bodyWaterMass else emptyList(),
            vo2Max = if ("VO2_MAX" in allowed) data.vo2Max else emptyList(),
            boneMass = if ("BONE_MASS" in allowed) data.boneMass else emptyList(),
            menstruationFlow = if ("MENSTRUATION_FLOW" in allowed) data.menstruationFlow else emptyList(),
            menstruationPeriod = if ("MENSTRUATION_PERIOD" in allowed) data.menstruationPeriod else emptyList(),
            intermenstrualBleeding = if ("INTERMENSTRUAL_BLEEDING" in allowed) data.intermenstrualBleeding else emptyList(),
            ovulationTest = if ("OVULATION_TEST" in allowed) data.ovulationTest else emptyList(),
            cervicalMucus = if ("CERVICAL_MUCUS" in allowed) data.cervicalMucus else emptyList(),
            sexualActivity = if ("SEXUAL_ACTIVITY" in allowed) data.sexualActivity else emptyList(),
            basalBodyTemperature = if ("BASAL_BODY_TEMPERATURE" in allowed) data.basalBodyTemperature else emptyList(),
            mindfulness = if ("MINDFULNESS" in allowed) data.mindfulness else emptyList(),
            deleted = data.deleted.filter { it.type.name in allowed },
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
                data.leanBodyMass.size + data.bodyWaterMass.size + data.vo2Max.size + data.boneMass.size +
                data.menstruationFlow.size + data.menstruationPeriod.size +
                data.intermenstrualBleeding.size + data.ovulationTest.size +
                data.cervicalMucus.size + data.sexualActivity.size + data.basalBodyTemperature.size +
                data.mindfulness.size +
                data.deleted.size
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
                data.bodyWaterMass.isEmpty() &&
                data.vo2Max.isEmpty() && data.boneMass.isEmpty() &&
                data.menstruationFlow.isEmpty() && data.menstruationPeriod.isEmpty() &&
                data.intermenstrualBleeding.isEmpty() && data.ovulationTest.isEmpty() &&
                data.cervicalMucus.isEmpty() && data.sexualActivity.isEmpty() && data.basalBodyTemperature.isEmpty() &&
                data.mindfulness.isEmpty() &&
                data.deleted.isEmpty()
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

    /**
     * Counts the records per type. Moves each type's last sync timestamp only when
     * [moveTimestamps] is true: a sync over an explicit range must not move it, or a range in
     * the past moves it backwards and later syncs send the same records again.
     */
    private fun updateSyncTimestamps(
        data: HealthData,
        syncCounts: MutableMap<HealthDataType, Int>,
        moveTimestamps: Boolean,
    ) {
        val now = Instant.now()
        fun setTimestamp(type: HealthDataType, timestamp: Long) {
            if (moveTimestamps) preferencesManager.setLastSyncTimestamp(type, timestamp)
        }
        if (data.steps.isNotEmpty()) {
            clampedMaxEndMs(data.steps.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.STEPS, it) }
            syncCounts[HealthDataType.STEPS] = data.steps.size
        }
        if (data.sleep.isNotEmpty()) {
            clampedMaxEndMs(data.sleep.asSequence().map { it.sessionEndTime }, now)
                ?.let { setTimestamp(HealthDataType.SLEEP, it) }
            syncCounts[HealthDataType.SLEEP] = data.sleep.size
        }
        if (data.heartRate.isNotEmpty()) {
            setTimestamp(HealthDataType.HEART_RATE, data.heartRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEART_RATE] = data.heartRate.size
        }
        if (data.heartRateVariability.isNotEmpty()) {
            setTimestamp(HealthDataType.HEART_RATE_VARIABILITY, data.heartRateVariability.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEART_RATE_VARIABILITY] = data.heartRateVariability.size
        }
        if (data.distance.isNotEmpty()) {
            clampedMaxEndMs(data.distance.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.DISTANCE, it) }
            syncCounts[HealthDataType.DISTANCE] = data.distance.size
        }
        if (data.activeCalories.isNotEmpty()) {
            clampedMaxEndMs(data.activeCalories.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.ACTIVE_CALORIES, it) }
            syncCounts[HealthDataType.ACTIVE_CALORIES] = data.activeCalories.size
        }
        if (data.totalCalories.isNotEmpty()) {
            // Clamp to now() so a future-dated endTime (e.g., daily projection
            // from Google Health ending at local midnight) cannot advance the
            // cursor past wall-clock time and starve subsequent closed records.
            clampedMaxEndMs(data.totalCalories.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.TOTAL_CALORIES, it) }
            syncCounts[HealthDataType.TOTAL_CALORIES] = data.totalCalories.size
        }
        if (data.weight.isNotEmpty()) {
            setTimestamp(HealthDataType.WEIGHT, data.weight.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.WEIGHT] = data.weight.size
        }
        if (data.height.isNotEmpty()) {
            setTimestamp(HealthDataType.HEIGHT, data.height.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.HEIGHT] = data.height.size
        }
        if (data.bloodPressure.isNotEmpty()) {
            setTimestamp(HealthDataType.BLOOD_PRESSURE, data.bloodPressure.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BLOOD_PRESSURE] = data.bloodPressure.size
        }
        if (data.bloodGlucose.isNotEmpty()) {
            setTimestamp(HealthDataType.BLOOD_GLUCOSE, data.bloodGlucose.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BLOOD_GLUCOSE] = data.bloodGlucose.size
        }
        if (data.oxygenSaturation.isNotEmpty()) {
            setTimestamp(HealthDataType.OXYGEN_SATURATION, data.oxygenSaturation.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.OXYGEN_SATURATION] = data.oxygenSaturation.size
        }
        if (data.bodyTemperature.isNotEmpty()) {
            setTimestamp(HealthDataType.BODY_TEMPERATURE, data.bodyTemperature.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BODY_TEMPERATURE] = data.bodyTemperature.size
        }
        if (data.skinTemperature.isNotEmpty()) {
            setTimestamp(HealthDataType.SKIN_TEMPERATURE, data.skinTemperature.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.SKIN_TEMPERATURE] = data.skinTemperature.size
        }
        if (data.respiratoryRate.isNotEmpty()) {
            setTimestamp(HealthDataType.RESPIRATORY_RATE, data.respiratoryRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.RESPIRATORY_RATE] = data.respiratoryRate.size
        }
        if (data.restingHeartRate.isNotEmpty()) {
            setTimestamp(HealthDataType.RESTING_HEART_RATE, data.restingHeartRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.RESTING_HEART_RATE] = data.restingHeartRate.size
        }
        if (data.exercise.isNotEmpty()) {
            clampedMaxEndMs(data.exercise.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.EXERCISE, it) }
            syncCounts[HealthDataType.EXERCISE] = data.exercise.size
        }
        if (data.hydration.isNotEmpty()) {
            clampedMaxEndMs(data.hydration.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.HYDRATION, it) }
            syncCounts[HealthDataType.HYDRATION] = data.hydration.size
        }
        if (data.nutrition.isNotEmpty()) {
            clampedMaxEndMs(data.nutrition.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.NUTRITION, it) }
            syncCounts[HealthDataType.NUTRITION] = data.nutrition.size
        }
        if (data.basalMetabolicRate.isNotEmpty()) {
            setTimestamp(HealthDataType.BASAL_METABOLIC_RATE, data.basalMetabolicRate.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BASAL_METABOLIC_RATE] = data.basalMetabolicRate.size
        }
        if (data.bodyFat.isNotEmpty()) {
            setTimestamp(HealthDataType.BODY_FAT, data.bodyFat.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BODY_FAT] = data.bodyFat.size
        }
        if (data.leanBodyMass.isNotEmpty()) {
            setTimestamp(HealthDataType.LEAN_BODY_MASS, data.leanBodyMass.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.LEAN_BODY_MASS] = data.leanBodyMass.size
        }
        if (data.bodyWaterMass.isNotEmpty()) {
            setTimestamp(HealthDataType.BODY_WATER_MASS, data.bodyWaterMass.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BODY_WATER_MASS] = data.bodyWaterMass.size
        }
        if (data.vo2Max.isNotEmpty()) {
            setTimestamp(HealthDataType.VO2_MAX, data.vo2Max.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.VO2_MAX] = data.vo2Max.size
        }
        if (data.boneMass.isNotEmpty()) {
            setTimestamp(HealthDataType.BONE_MASS, data.boneMass.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BONE_MASS] = data.boneMass.size
        }
        if (data.menstruationFlow.isNotEmpty()) {
            setTimestamp(HealthDataType.MENSTRUATION_FLOW, data.menstruationFlow.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.MENSTRUATION_FLOW] = data.menstruationFlow.size
        }
        if (data.menstruationPeriod.isNotEmpty()) {
            clampedMaxEndMs(data.menstruationPeriod.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.MENSTRUATION_PERIOD, it) }
            syncCounts[HealthDataType.MENSTRUATION_PERIOD] = data.menstruationPeriod.size
        }
        if (data.intermenstrualBleeding.isNotEmpty()) {
            setTimestamp(HealthDataType.INTERMENSTRUAL_BLEEDING, data.intermenstrualBleeding.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.INTERMENSTRUAL_BLEEDING] = data.intermenstrualBleeding.size
        }
        if (data.ovulationTest.isNotEmpty()) {
            setTimestamp(HealthDataType.OVULATION_TEST, data.ovulationTest.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.OVULATION_TEST] = data.ovulationTest.size
        }
        if (data.cervicalMucus.isNotEmpty()) {
            setTimestamp(HealthDataType.CERVICAL_MUCUS, data.cervicalMucus.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.CERVICAL_MUCUS] = data.cervicalMucus.size
        }
        if (data.sexualActivity.isNotEmpty()) {
            setTimestamp(HealthDataType.SEXUAL_ACTIVITY, data.sexualActivity.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.SEXUAL_ACTIVITY] = data.sexualActivity.size
        }
        if (data.basalBodyTemperature.isNotEmpty()) {
            setTimestamp(HealthDataType.BASAL_BODY_TEMPERATURE, data.basalBodyTemperature.maxOf { it.time }.toEpochMilli())
            syncCounts[HealthDataType.BASAL_BODY_TEMPERATURE] = data.basalBodyTemperature.size
        }
        if (data.mindfulness.isNotEmpty()) {
            clampedMaxEndMs(data.mindfulness.asSequence().map { it.endTime }, now)
                ?.let { setTimestamp(HealthDataType.MINDFULNESS, it) }
            syncCounts[HealthDataType.MINDFULNESS] = data.mindfulness.size
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
        if (data.mindfulness.isNotEmpty()) {
            parts.add("${data.mindfulness.size} mindfulness")
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
        if (data.deleted.isNotEmpty()) {
            parts.add("${data.deleted.size} deleted")
        }

        return if (parts.isEmpty()) "No new data" else parts.joinToString(" · ")
    }

    private fun buildJsonPayload(healthData: HealthData): String {
        val time = preferencesManager.payloadTime()
        val json = buildJsonObject {
            put("timestamp", time.format(Instant.now()))
            put("app_version", appVersionName)
            put("timezone", time.timezoneName)

            if (healthData.steps.isNotEmpty()) {
                putJsonArray("steps") {
                    healthData.steps.forEach { step ->
                        add(buildJsonObject {
                            put("count", step.count)
                            put("start_time", time.format(step.startTime))
                            put("end_time", time.format(step.endTime))
                            step.metadata?.let { meta -> putRecordMetadata(meta, time) }
                        })
                    }
                }
            }

            if (healthData.sleep.isNotEmpty()) {
                putJsonArray("sleep") {
                    healthData.sleep.forEach { sleep ->
                        add(buildJsonObject {
                            put("session_end_time", time.format(sleep.sessionEndTime))
                            put("duration_seconds", sleep.duration.seconds)
                            putJsonArray("stages") {
                                sleep.stages.forEach { stage ->
                                    add(buildJsonObject {
                                        put("stage", stage.stage)
                                        put("start_time", time.format(stage.startTime))
                                        put("end_time", time.format(stage.endTime))
                                        put("duration_seconds", stage.duration.seconds)
                                    })
                                }
                            }
                            sleep.metadata?.let { meta -> putRecordMetadata(meta, time) }
                        })
                    }
                }
            }

            if (healthData.heartRate.isNotEmpty()) {
                putJsonArray("heart_rate") {
                    healthData.heartRate.forEach { hr -> add(buildJsonObject {
                        if (hr.min != null) {
                            put("time", time.format(hr.time))
                            put("avg", hr.bpm)
                            put("min", hr.min)
                            put("max", hr.max)
                            put("bpm", hr.bpm)
                        } else {
                            put("bpm", hr.bpm)
                            put("time", time.format(hr.time))
                        }
                        hr.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.heartRateVariability.isNotEmpty()) {
                putJsonArray("heart_rate_variability") {
                    healthData.heartRateVariability.forEach { hrv -> add(buildJsonObject {
                        if (hrv.min != null) {
                            put("time", time.format(hrv.time))
                            put("avg", hrv.rmssdMillis)
                            put("min", hrv.min)
                            put("max", hrv.max)
                            put("rmssd_millis", hrv.rmssdMillis)
                        } else {
                            put("rmssd_millis", hrv.rmssdMillis)
                            put("time", time.format(hrv.time))
                        }
                        hrv.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.distance.isNotEmpty()) {
                putJsonArray("distance") {
                    healthData.distance.forEach { add(buildJsonObject {
                        put("meters", it.meters)
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.activeCalories.isNotEmpty()) {
                putJsonArray("active_calories") {
                    healthData.activeCalories.forEach { add(buildJsonObject {
                        put("calories", it.calories)
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.totalCalories.isNotEmpty()) {
                putJsonArray("total_calories") {
                    healthData.totalCalories.forEach { add(buildJsonObject {
                        put("calories", it.calories)
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.weight.isNotEmpty()) {
                putJsonArray("weight") {
                    healthData.weight.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.height.isNotEmpty()) {
                putJsonArray("height") {
                    healthData.height.forEach { add(buildJsonObject {
                        put("meters", it.meters)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.bloodPressure.isNotEmpty()) {
                putJsonArray("blood_pressure") {
                    healthData.bloodPressure.forEach { add(buildJsonObject {
                        put("systolic", it.systolic)
                        put("diastolic", it.diastolic)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.bloodGlucose.isNotEmpty()) {
                putJsonArray("blood_glucose") {
                    healthData.bloodGlucose.forEach { add(buildJsonObject {
                        put("mmol_per_liter", it.mmolPerLiter)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.oxygenSaturation.isNotEmpty()) {
                putJsonArray("oxygen_saturation") {
                    healthData.oxygenSaturation.forEach { o2 -> add(buildJsonObject {
                        if (o2.min != null) {
                            put("time", time.format(o2.time))
                            put("avg", o2.percentage)
                            put("min", o2.min)
                            put("max", o2.max)
                            put("percentage", o2.percentage)
                        } else {
                            put("percentage", o2.percentage)
                            put("time", time.format(o2.time))
                        }
                        o2.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.bodyTemperature.isNotEmpty()) {
                putJsonArray("body_temperature") {
                    healthData.bodyTemperature.forEach { add(buildJsonObject {
                        put("celsius", it.celsius)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.skinTemperature.isNotEmpty()) {
                putJsonArray("skin_temperature") {
                    healthData.skinTemperature.forEach { skin -> add(buildJsonObject {
                        put("time", time.format(skin.time))
                        if (skin.minDeltaCelsius != null) {
                            put("avg_delta_celsius", skin.deltaCelsius)
                            put("min_delta_celsius", skin.minDeltaCelsius)
                            put("max_delta_celsius", skin.maxDeltaCelsius)
                            put("delta_celsius", skin.deltaCelsius)
                        } else {
                            put("delta_celsius", skin.deltaCelsius)
                        }
                        skin.baselineCelsius?.let { baseline -> put("baseline_celsius", baseline) }
                        put("measurement_location", skin.measurementLocation)
                        skin.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.respiratoryRate.isNotEmpty()) {
                putJsonArray("respiratory_rate") {
                    healthData.respiratoryRate.forEach { resp -> add(buildJsonObject {
                        if (resp.min != null) {
                            put("time", time.format(resp.time))
                            put("avg", resp.rate)
                            put("min", resp.min)
                            put("max", resp.max)
                            put("rate", resp.rate)
                        } else {
                            put("rate", resp.rate)
                            put("time", time.format(resp.time))
                        }
                        resp.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.restingHeartRate.isNotEmpty()) {
                putJsonArray("resting_heart_rate") {
                    healthData.restingHeartRate.forEach { add(buildJsonObject {
                        put("bpm", it.bpm)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.exercise.isNotEmpty()) {
                putJsonArray("exercise") {
                    healthData.exercise.forEach { add(buildJsonObject {
                        put("type", it.type)
                        it.title?.let { title -> put("title", title) }
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
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
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.hydration.isNotEmpty()) {
                putJsonArray("hydration") {
                    healthData.hydration.forEach { add(buildJsonObject {
                        put("liters", it.liters)
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
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
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.basalMetabolicRate.isNotEmpty()) {
                putJsonArray("basal_metabolic_rate") {
                    healthData.basalMetabolicRate.forEach { add(buildJsonObject {
                        put("watts", it.watts)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.bodyFat.isNotEmpty()) {
                putJsonArray("body_fat") {
                    healthData.bodyFat.forEach { add(buildJsonObject {
                        put("percentage", it.percentage)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.leanBodyMass.isNotEmpty()) {
                putJsonArray("lean_body_mass") {
                    healthData.leanBodyMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.bodyWaterMass.isNotEmpty()) {
                putJsonArray("body_water_mass") {
                    healthData.bodyWaterMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            // Health Connect has no BMI record type. Compute BMI = kg / m² when both
            // weight and height lists are present (enabled + non-empty after filter).
            // Pair each weight with the height closest in time.
            if (healthData.weight.isNotEmpty() && healthData.height.isNotEmpty()) {
                putJsonArray("bmi") {
                    computeBmiEntries(healthData.weight, healthData.height).forEach { entry ->
                        add(buildJsonObject {
                            put("value", entry.value)
                            put("time", time.format(entry.time))
                            put("weight_kg", entry.weightKg)
                            put("height_meters", entry.heightMeters)
                        })
                    }
                }
            }

            if (healthData.vo2Max.isNotEmpty()) {
                putJsonArray("vo2_max") {
                    healthData.vo2Max.forEach { add(buildJsonObject {
                        put("ml_per_kg_per_min", it.mlPerKgPerMin)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.boneMass.isNotEmpty()) {
                putJsonArray("bone_mass") {
                    healthData.boneMass.forEach { add(buildJsonObject {
                        put("kilograms", it.kilograms)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.menstruationFlow.isNotEmpty()) {
                putJsonArray("menstruation_flow") {
                    healthData.menstruationFlow.forEach { add(buildJsonObject {
                        put("flow", it.flow)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.menstruationPeriod.isNotEmpty()) {
                putJsonArray("menstruation_period") {
                    healthData.menstruationPeriod.forEach { add(buildJsonObject {
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.intermenstrualBleeding.isNotEmpty()) {
                putJsonArray("intermenstrual_bleeding") {
                    healthData.intermenstrualBleeding.forEach { add(buildJsonObject {
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.ovulationTest.isNotEmpty()) {
                putJsonArray("ovulation_test") {
                    healthData.ovulationTest.forEach { add(buildJsonObject {
                        put("result", it.result)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.cervicalMucus.isNotEmpty()) {
                putJsonArray("cervical_mucus") {
                    healthData.cervicalMucus.forEach { add(buildJsonObject {
                        put("appearance", it.appearance)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.sexualActivity.isNotEmpty()) {
                putJsonArray("sexual_activity") {
                    healthData.sexualActivity.forEach { add(buildJsonObject {
                        put("protection_used", it.protectionUsed)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.basalBodyTemperature.isNotEmpty()) {
                putJsonArray("basal_body_temperature") {
                    healthData.basalBodyTemperature.forEach { add(buildJsonObject {
                        put("celsius", it.celsius)
                        put("measurement_location", it.measurementLocation)
                        put("time", time.format(it.time))
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.mindfulness.isNotEmpty()) {
                putJsonArray("mindfulness") {
                    healthData.mindfulness.forEach { add(buildJsonObject {
                        put("session_type", it.sessionType)
                        it.title?.let { title -> put("title", title) }
                        it.notes?.let { notes -> put("notes", notes) }
                        put("start_time", time.format(it.startTime))
                        put("end_time", time.format(it.endTime))
                        put("duration_seconds", it.duration.seconds)
                        it.metadata?.let { meta -> putRecordMetadata(meta, time) }
                    }) }
                }
            }

            if (healthData.deleted.isNotEmpty()) {
                putJsonArray("deleted") {
                    healthData.deleted.forEach { add(buildJsonObject {
                        put("type", it.type.name.lowercase())
                        put("id", it.id)
                    }) }
                }
            }
        } // End of buildJsonObject block

        return json.toString()
    }

    companion object {
        /**
         * Soft cap before JSON encode. Full-resolution heart rate over 48h can
         * exceed this and OOMs mid-tier devices while building JsonObject trees.
         */
        private const val MAX_RECORDS_PER_PAYLOAD = 25_000
        private const val TAG = "SyncManager"
    }

    private data class BmiEntry(
        val value: Double,
        val time: Instant,
        val weightKg: Double,
        val heightMeters: Double
    )

    /** BMI = kg / m². Pair each weight with the height record closest in time. */
    private fun computeBmiEntries(
        weights: List<WeightData>,
        heights: List<HeightData>
    ): List<BmiEntry> {
        if (weights.isEmpty() || heights.isEmpty()) return emptyList()
        return weights.mapNotNull { weight ->
            val height = heights.minByOrNull { h ->
                kotlin.math.abs(h.time.toEpochMilli() - weight.time.toEpochMilli())
            } ?: return@mapNotNull null
            if (height.meters <= 0.0) return@mapNotNull null
            val bmi = weight.kilograms / (height.meters * height.meters)
            BmiEntry(
                value = bmi,
                time = weight.time,
                weightKg = weight.kilograms,
                heightMeters = height.meters
            )
        }
    }
}

sealed class SyncResult {
    object NoData : SyncResult()
    object NoMatchingData : SyncResult()
    data class Success(val syncCounts: Map<HealthDataType, Int>) : SyncResult()
}
