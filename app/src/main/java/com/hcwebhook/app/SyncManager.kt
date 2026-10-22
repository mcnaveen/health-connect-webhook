package com.hcwebhook.app

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.hcwebhook.app.proto.v1.HealthPayload
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
import java.io.File
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
    private val outbox = WebhookOutbox(File(context.filesDir, "webhook_outbox"))

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

            // Payloads a webhook missed in earlier syncs go out first, so it gets them in order.
            outbox.keepOnly(webhookConfigs.map { it.url })
            val outboxClear = enabledWebhookConfigs.associateWith { drainOutbox(it) }

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
            val typeChanges = if (!hasExplicitRange) {
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
            val healthDataResult = healthConnectManager.readHealthData(
                enabledTypes = enabledTypes,
                lastSyncTimestamps = lastSyncTimestamps,
                timeRangeDays = timeRangeDays,
                start = start,
                end = readEnd,
                dataTypeResolutions = preferencesManager.getDataTypeResolutions(),
                changes = typeChanges.mapValues { (type, changes) ->
                    changes.changed.copy(sentAtLastSync = preferencesManager.getLastSyncBoundaryIds(type))
                },
            )
            if (healthDataResult.isFailure) {
                return@withContext Result.failure(healthDataResult.exceptionOrNull() ?: Exception("Failed to read health data"))
            }
            val healthData = healthDataResult.getOrThrow()
                .copy(deleted = typeChanges.deletedRecords())
                .withoutDeletionsOfPresentRecords()

            // Check if there's any new data
            if (isHealthDataEmpty(healthData)) {
                saveChangesTokens(typeChanges)
                preferencesManager.setLastSyncTime(Instant.now().toEpochMilli())
                preferencesManager.setLastSyncSummary("No new data")
                return@withContext Result.success(SyncResult.NoData)
            }

            val itemsText = describeItems(healthData)

            // Build full payload (also used by local TCP server). Data too large for one payload
            // is sent in parts, and each part is built on its own, so the full JSON of a big
            // backlog never has to fit in memory. The local server then gets the last part.
            val fullPayload = if (healthData.itemCount() <= MAX_ITEMS_PER_PAYLOAD) {
                try {
                    buildJsonPayload(healthData)
                } catch (oom: OutOfMemoryError) {
                    return@withContext Result.failure(
                        Exception(
                            "Out of memory while building JSON ($itemsText). " +
                                "Raise sample resolution or sync a shorter range.",
                            oom
                        )
                    )
                }
            } else {
                null
            }
            val latestPayload = fullPayload ?: try {
                buildJsonPayload(healthData.chunked(MAX_ITEMS_PER_PAYLOAD).last())
            } catch (_: OutOfMemoryError) {
                null
            }
            latestPayload?.let(LocalHttpServerManager::publishPayload)

            /** Posts one payload to one webhook. [json] is the payload when it is already built. */
            suspend fun deliver(config: WebhookConfig, data: HealthData, json: String?): Result<Unit> {
                val itemCount = data.itemCount()
                return when (config.deliveryFormat) {
                    WebhookDeliveryFormat.JSON -> {
                        val payload = try {
                            json ?: buildJsonPayload(data)
                        } catch (oom: OutOfMemoryError) {
                            return Result.failure(
                                Exception("Out of memory while building JSON for ${config.url}. Raise sample resolution.", oom)
                            )
                        }
                        WebhookManager(
                            webhookConfigs = listOf(config),
                            context = context,
                            dataType = "all",
                            recordCount = itemCount,
                            syncType = syncType,
                            payload = payload
                        ).postData(payload)
                    }
                    WebhookDeliveryFormat.GRPC -> {
                        val grpcPayload = try {
                            ProtobufPayloadBuilder.build(data, appVersionName)
                        } catch (oom: OutOfMemoryError) {
                            return Result.failure(
                                Exception("Out of memory while building protobuf for ${config.url}. Raise sample resolution.", oom)
                            )
                        }
                        val logJson = try {
                            json ?: buildJsonPayload(data)
                        } catch (_: OutOfMemoryError) {
                            null
                        }
                        GrpcWebhookClient.deliver(
                            config = config,
                            payload = grpcPayload,
                            context = context,
                            dataType = "all",
                            recordCount = itemCount,
                            syncType = syncType,
                            logPayload = logJson
                        )
                    }
                }
            }

            // Post to each enabled webhook with optional per-webhook data type filtering
            if (enabledWebhookConfigs.isNotEmpty()) {
                var atLeastOneSuccess = false
                var atLeastOneAttempted = false
                var lastFailure: Throwable? = null

                val dispatcher = NotificationDispatcher()
                val globalNotifs = preferencesManager.getNotificationConfigs()
                val aggregatedNotifs = mutableMapOf<NotificationConfig, MutableList<String>>()
                // Payloads each webhook did not get in this sync.
                val missed = mutableMapOf<WebhookConfig, List<HealthData>>()

                for (config in enabledWebhookConfigs) {
                    val filteredData = if (config.dataTypeFilter != null) {
                        filterHealthData(healthData, config.dataTypeFilter)
                    } else {
                        healthData
                    }
                    if (isHealthDataEmpty(filteredData)) continue
                    atLeastOneAttempted = true
                    val totalRecords = filteredData.itemCount()

                    // Data too large for one request goes out as several payloads, so a big
                    // backlog cannot fail every sync. A failed part stops this webhook; the next
                    // sync sends all parts again, and a receiver upserts by id or time.
                    val chunks = filteredData.chunked(MAX_ITEMS_PER_PAYLOAD)
                    if (chunks.size > 1) {
                        Log.i(TAG, "Sending $totalRecords items to ${config.url} in ${chunks.size} payloads")
                    }
                    var result: Result<Unit> = Result.success(Unit)
                    if (outboxClear[config] == false) {
                        // Older payloads still wait for this webhook; new ones queue behind them.
                        result = Result.failure(Exception("Earlier payloads for ${config.url} are still waiting"))
                        missed[config] = chunks
                    } else {
                        for ((index, chunk) in chunks.withIndex()) {
                            val json = if (chunks.size == 1 && config.dataTypeFilter == null) fullPayload else null
                            result = deliver(config, chunk, json)
                            if (result.isFailure) {
                                missed[config] = chunks.drop(index)
                                break
                            }
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
                // The sync moves its timestamps and tokens now, so a webhook that failed would never
                // get this data. Keep its payloads for the next sync.
                missed.forEach { (config, chunks) -> queueForLater(config, chunks) }
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

    /**
     * Sends [config]'s waiting payloads, oldest first, and returns true when none are left. Stops
     * at the first failure, so the payloads keep their order.
     */
    private suspend fun drainOutbox(config: WebhookConfig): Boolean {
        for (item in outbox.items(config.url)) {
            if (item.format != config.deliveryFormat) {
                // The webhook changed its format, so the stored payload no longer fits it.
                Log.w(TAG, "Dropping a waiting ${item.format} payload for ${config.url}, now ${config.deliveryFormat}")
                outbox.remove(item)
                continue
            }
            val bytes = outbox.read(item)
            val result = when (item.format) {
                WebhookDeliveryFormat.JSON -> {
                    val json = bytes.toString(Charsets.UTF_8)
                    WebhookManager(
                        webhookConfigs = listOf(config),
                        context = context,
                        dataType = "all",
                        recordCount = item.itemCount,
                        syncType = OUTBOX_SYNC_TYPE,
                        payload = json
                    ).postData(json)
                }
                WebhookDeliveryFormat.GRPC -> GrpcWebhookClient.deliver(
                    config = config,
                    payload = HealthPayload.parseFrom(bytes),
                    context = context,
                    dataType = "all",
                    recordCount = item.itemCount,
                    syncType = OUTBOX_SYNC_TYPE
                )
            }
            if (result.isFailure) return false
            outbox.remove(item)
        }
        return true
    }

    /** Stores [chunks] for [config] in the outbox, in the webhook's own format. */
    private fun queueForLater(config: WebhookConfig, chunks: List<HealthData>) {
        for (chunk in chunks) {
            val bytes = try {
                when (config.deliveryFormat) {
                    WebhookDeliveryFormat.JSON -> buildJsonPayload(chunk).toByteArray(Charsets.UTF_8)
                    WebhookDeliveryFormat.GRPC -> ProtobufPayloadBuilder.build(chunk, appVersionName).toByteArray()
                }
            } catch (oom: OutOfMemoryError) {
                Log.w(TAG, "Out of memory while storing a payload for ${config.url}", oom)
                return
            }
            val dropped = outbox.add(config.url, config.deliveryFormat, bytes, chunk.itemCount())
            if (dropped > 0) Log.w(TAG, "Outbox for ${config.url} is full; dropped the $dropped oldest payloads")
        }
        Log.i(TAG, "Kept ${chunks.size} payloads for ${config.url} until the next sync")
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
                data.mindfulness.size
    }

    /** "N records" or "N records, M deleted", for messages. */
    private fun describeItems(data: HealthData): String {
        val records = "${countHealthData(data)} records"
        return if (data.deleted.isEmpty()) records else "$records, ${data.deleted.size} deleted"
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
        /**
         * Moves [type]'s timestamp to the newest of [items] (time, record id) and stores the ids
         * sent at that millisecond. With [clamp], future times are skipped (see [clampedMaxEndMs]).
         */
        fun mark(type: HealthDataType, items: List<Pair<Instant, String?>>, clamp: Boolean) {
            if (!moveTimestamps) return
            val timestamp = if (clamp) {
                clampedMaxEndMs(items.asSequence().map { it.first }, now) ?: return
            } else {
                items.maxOf { it.first }.toEpochMilli()
            }
            val boundaryIds = items.filter { it.first.toEpochMilli() == timestamp }.mapNotNullTo(HashSet()) { it.second }
            preferencesManager.setLastSyncTimestamp(type, timestamp, boundaryIds)
        }
        if (data.steps.isNotEmpty()) {
            mark(HealthDataType.STEPS, data.steps.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.STEPS] = data.steps.size
        }
        if (data.sleep.isNotEmpty()) {
            mark(HealthDataType.SLEEP, data.sleep.map { it.sessionEndTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.SLEEP] = data.sleep.size
        }
        if (data.heartRate.isNotEmpty()) {
            mark(HealthDataType.HEART_RATE, data.heartRate.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.HEART_RATE] = data.heartRate.size
        }
        if (data.heartRateVariability.isNotEmpty()) {
            mark(HealthDataType.HEART_RATE_VARIABILITY, data.heartRateVariability.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.HEART_RATE_VARIABILITY] = data.heartRateVariability.size
        }
        if (data.distance.isNotEmpty()) {
            mark(HealthDataType.DISTANCE, data.distance.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.DISTANCE] = data.distance.size
        }
        if (data.activeCalories.isNotEmpty()) {
            mark(HealthDataType.ACTIVE_CALORIES, data.activeCalories.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.ACTIVE_CALORIES] = data.activeCalories.size
        }
        if (data.totalCalories.isNotEmpty()) {
            // Clamp to now() so a future-dated endTime (e.g., daily projection
            // from Google Health ending at local midnight) cannot advance the
            // cursor past wall-clock time and starve subsequent closed records.
            mark(HealthDataType.TOTAL_CALORIES, data.totalCalories.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.TOTAL_CALORIES] = data.totalCalories.size
        }
        if (data.weight.isNotEmpty()) {
            mark(HealthDataType.WEIGHT, data.weight.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.WEIGHT] = data.weight.size
        }
        if (data.height.isNotEmpty()) {
            mark(HealthDataType.HEIGHT, data.height.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.HEIGHT] = data.height.size
        }
        if (data.bloodPressure.isNotEmpty()) {
            mark(HealthDataType.BLOOD_PRESSURE, data.bloodPressure.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BLOOD_PRESSURE] = data.bloodPressure.size
        }
        if (data.bloodGlucose.isNotEmpty()) {
            mark(HealthDataType.BLOOD_GLUCOSE, data.bloodGlucose.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BLOOD_GLUCOSE] = data.bloodGlucose.size
        }
        if (data.oxygenSaturation.isNotEmpty()) {
            mark(HealthDataType.OXYGEN_SATURATION, data.oxygenSaturation.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.OXYGEN_SATURATION] = data.oxygenSaturation.size
        }
        if (data.bodyTemperature.isNotEmpty()) {
            mark(HealthDataType.BODY_TEMPERATURE, data.bodyTemperature.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BODY_TEMPERATURE] = data.bodyTemperature.size
        }
        if (data.skinTemperature.isNotEmpty()) {
            mark(HealthDataType.SKIN_TEMPERATURE, data.skinTemperature.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.SKIN_TEMPERATURE] = data.skinTemperature.size
        }
        if (data.respiratoryRate.isNotEmpty()) {
            mark(HealthDataType.RESPIRATORY_RATE, data.respiratoryRate.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.RESPIRATORY_RATE] = data.respiratoryRate.size
        }
        if (data.restingHeartRate.isNotEmpty()) {
            mark(HealthDataType.RESTING_HEART_RATE, data.restingHeartRate.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.RESTING_HEART_RATE] = data.restingHeartRate.size
        }
        if (data.exercise.isNotEmpty()) {
            mark(HealthDataType.EXERCISE, data.exercise.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.EXERCISE] = data.exercise.size
        }
        if (data.hydration.isNotEmpty()) {
            mark(HealthDataType.HYDRATION, data.hydration.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.HYDRATION] = data.hydration.size
        }
        if (data.nutrition.isNotEmpty()) {
            mark(HealthDataType.NUTRITION, data.nutrition.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.NUTRITION] = data.nutrition.size
        }
        if (data.basalMetabolicRate.isNotEmpty()) {
            mark(HealthDataType.BASAL_METABOLIC_RATE, data.basalMetabolicRate.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BASAL_METABOLIC_RATE] = data.basalMetabolicRate.size
        }
        if (data.bodyFat.isNotEmpty()) {
            mark(HealthDataType.BODY_FAT, data.bodyFat.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BODY_FAT] = data.bodyFat.size
        }
        if (data.leanBodyMass.isNotEmpty()) {
            mark(HealthDataType.LEAN_BODY_MASS, data.leanBodyMass.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.LEAN_BODY_MASS] = data.leanBodyMass.size
        }
        if (data.bodyWaterMass.isNotEmpty()) {
            mark(HealthDataType.BODY_WATER_MASS, data.bodyWaterMass.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BODY_WATER_MASS] = data.bodyWaterMass.size
        }
        if (data.vo2Max.isNotEmpty()) {
            mark(HealthDataType.VO2_MAX, data.vo2Max.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.VO2_MAX] = data.vo2Max.size
        }
        if (data.boneMass.isNotEmpty()) {
            mark(HealthDataType.BONE_MASS, data.boneMass.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BONE_MASS] = data.boneMass.size
        }
        if (data.menstruationFlow.isNotEmpty()) {
            mark(HealthDataType.MENSTRUATION_FLOW, data.menstruationFlow.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.MENSTRUATION_FLOW] = data.menstruationFlow.size
        }
        if (data.menstruationPeriod.isNotEmpty()) {
            mark(HealthDataType.MENSTRUATION_PERIOD, data.menstruationPeriod.map { it.endTime to it.metadata?.id }, clamp = true)
            syncCounts[HealthDataType.MENSTRUATION_PERIOD] = data.menstruationPeriod.size
        }
        if (data.intermenstrualBleeding.isNotEmpty()) {
            mark(HealthDataType.INTERMENSTRUAL_BLEEDING, data.intermenstrualBleeding.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.INTERMENSTRUAL_BLEEDING] = data.intermenstrualBleeding.size
        }
        if (data.ovulationTest.isNotEmpty()) {
            mark(HealthDataType.OVULATION_TEST, data.ovulationTest.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.OVULATION_TEST] = data.ovulationTest.size
        }
        if (data.cervicalMucus.isNotEmpty()) {
            mark(HealthDataType.CERVICAL_MUCUS, data.cervicalMucus.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.CERVICAL_MUCUS] = data.cervicalMucus.size
        }
        if (data.sexualActivity.isNotEmpty()) {
            mark(HealthDataType.SEXUAL_ACTIVITY, data.sexualActivity.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.SEXUAL_ACTIVITY] = data.sexualActivity.size
        }
        if (data.basalBodyTemperature.isNotEmpty()) {
            mark(HealthDataType.BASAL_BODY_TEMPERATURE, data.basalBodyTemperature.map { it.time to it.metadata?.id }, clamp = false)
            syncCounts[HealthDataType.BASAL_BODY_TEMPERATURE] = data.basalBodyTemperature.size
        }
        if (data.mindfulness.isNotEmpty()) {
            mark(HealthDataType.MINDFULNESS, data.mindfulness.map { it.endTime to it.metadata?.id }, clamp = true)
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
                        it.mealType?.let { mealType -> put("meal_type", mealType) }
                        it.nutrients.forEach { (key, value) -> put(key, value) }
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
        private const val MAX_ITEMS_PER_PAYLOAD = 25_000
        /** syncType of the webhook log entries of outbox payloads. */
        private const val OUTBOX_SYNC_TYPE = "outbox"
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
