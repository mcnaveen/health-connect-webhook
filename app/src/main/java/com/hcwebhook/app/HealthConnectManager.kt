package com.hcwebhook.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

enum class HealthDataType(val nameResId: Int, val recordClass: KClass<out Record>, val rationaleResId: Int) {
    STEPS(
        R.string.dt_steps_name, StepsRecord::class, R.string.dt_steps_rationale
    ),
    SLEEP(
        R.string.dt_sleep_name, SleepSessionRecord::class, R.string.dt_sleep_rationale
    ),
    HEART_RATE(
        R.string.dt_hr_name, HeartRateRecord::class, R.string.dt_hr_rationale
    ),
    HEART_RATE_VARIABILITY(
        R.string.dt_hrv_name, HeartRateVariabilityRmssdRecord::class, R.string.dt_hrv_rationale
    ),
    DISTANCE(
        R.string.dt_dist_name, DistanceRecord::class, R.string.dt_dist_rationale
    ),
    ACTIVE_CALORIES(
        R.string.dt_act_cal_name, ActiveCaloriesBurnedRecord::class, R.string.dt_act_cal_rationale
    ),
    TOTAL_CALORIES(
        R.string.dt_tot_cal_name, TotalCaloriesBurnedRecord::class, R.string.dt_tot_cal_rationale
    ),
    WEIGHT(
        R.string.dt_weight_name, WeightRecord::class, R.string.dt_weight_rationale
    ),
    HEIGHT(
        R.string.dt_height_name, HeightRecord::class, R.string.dt_height_rationale
    ),
    BLOOD_PRESSURE(
        R.string.dt_bp_name, BloodPressureRecord::class, R.string.dt_bp_rationale
    ),
    BLOOD_GLUCOSE(
        R.string.dt_bg_name, BloodGlucoseRecord::class, R.string.dt_bg_rationale
    ),
    OXYGEN_SATURATION(
        R.string.dt_oxy_name, OxygenSaturationRecord::class, R.string.dt_oxy_rationale
    ),
    BODY_TEMPERATURE(
        R.string.dt_temp_name, BodyTemperatureRecord::class, R.string.dt_temp_rationale
    ),
    RESPIRATORY_RATE(
        R.string.dt_resp_name, RespiratoryRateRecord::class, R.string.dt_resp_rationale
    ),
    RESTING_HEART_RATE(
        R.string.dt_rhr_name, RestingHeartRateRecord::class, R.string.dt_rhr_rationale
    ),
    EXERCISE(
        R.string.dt_exer_name, ExerciseSessionRecord::class, R.string.dt_exer_rationale
    ),
    HYDRATION(
        R.string.dt_hydr_name, HydrationRecord::class, R.string.dt_hydr_rationale
    ),
    NUTRITION(
        R.string.dt_nutr_name, NutritionRecord::class, R.string.dt_nutr_rationale
    ),
    BASAL_METABOLIC_RATE(
        R.string.dt_bmr_name, BasalMetabolicRateRecord::class, R.string.dt_bmr_rationale
    ),
    BODY_FAT(
        R.string.dt_bf_name, BodyFatRecord::class, R.string.dt_bf_rationale
    ),
    LEAN_BODY_MASS(
        R.string.dt_lbm_name, LeanBodyMassRecord::class, R.string.dt_lbm_rationale
    ),
    VO2_MAX(
        R.string.dt_vo2_name, Vo2MaxRecord::class, R.string.dt_vo2_rationale
    ),
    BONE_MASS(
        R.string.dt_bone_name, BoneMassRecord::class, R.string.dt_bone_rationale
    )
}

data class HealthData(
    val steps: List<StepsData>,
    val sleep: List<SleepData>,
    val heartRate: List<HeartRateData>,
    val heartRateVariability: List<HeartRateVariabilityData>,
    val distance: List<DistanceData>,
    val activeCalories: List<ActiveCaloriesData>,
    val totalCalories: List<TotalCaloriesData>,
    val weight: List<WeightData>,
    val height: List<HeightData>,
    val bloodPressure: List<BloodPressureData>,
    val bloodGlucose: List<BloodGlucoseData>,
    val oxygenSaturation: List<OxygenSaturationData>,
    val bodyTemperature: List<BodyTemperatureData>,
    val respiratoryRate: List<RespiratoryRateData>,
    val restingHeartRate: List<RestingHeartRateData>,
    val exercise: List<ExerciseData>,
    val hydration: List<HydrationData>,
    val nutrition: List<NutritionData>,
    val basalMetabolicRate: List<BasalMetabolicRateData>,
    val bodyFat: List<BodyFatData>,
    val leanBodyMass: List<LeanBodyMassData>,
    val vo2Max: List<Vo2MaxData>,
    val boneMass: List<BoneMassData>
)

data class StepsData(
    val count: Long,
    val startTime: Instant,
    val endTime: Instant
)

data class SleepData(
    val sessionEndTime: Instant,
    val duration: Duration,
    val stages: List<SleepStage>
)

data class SleepStage(
    val stage: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration
)

data class HeartRateData(
    val bpm: Long,
    val time: Instant
)

data class HeartRateVariabilityData(
    val rmssdMillis: Double,
    val time: Instant
)

data class DistanceData(
    val meters: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class ActiveCaloriesData(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class TotalCaloriesData(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class WeightData(
    val kilograms: Double,
    val time: Instant
)

data class HeightData(
    val meters: Double,
    val time: Instant
)

data class BloodPressureData(
    val systolic: Double,
    val diastolic: Double,
    val time: Instant
)

data class BloodGlucoseData(
    val mmolPerLiter: Double,
    val time: Instant
)

data class OxygenSaturationData(
    val percentage: Double,
    val time: Instant
)

data class BodyTemperatureData(
    val celsius: Double,
    val time: Instant
)

data class RespiratoryRateData(
    val rate: Double,
    val time: Instant
)

data class RestingHeartRateData(
    val bpm: Long,
    val time: Instant
)

data class ExerciseData(
    val type: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val distanceMeters: Double? = null
)

data class HydrationData(
    val liters: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class NutritionData(
    val calories: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val startTime: Instant,
    val endTime: Instant
)

data class BasalMetabolicRateData(
    val watts: Double,
    val time: Instant
)

data class BodyFatData(
    val percentage: Double,
    val time: Instant
)

data class LeanBodyMassData(
    val kilograms: Double,
    val time: Instant
)

data class Vo2MaxData(
    val mlPerKgPerMin: Double,
    val time: Instant
)

data class BoneMassData(
    val kilograms: Double,
    val time: Instant
)

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            throw IllegalStateException("Health Connect is not available on this device: ${e.message}", e)
        }
    }

    suspend fun readHealthData(
        enabledTypes: Set<HealthDataType>,
        lastSyncTimestamps: Map<HealthDataType, Instant?>,
        timeRangeDays: Int? = null,
        start: Instant? = null,
        end: Instant? = null
    ): Result<HealthData> {
        return try {
            val endTime = end ?: Instant.now()
            val startTime = when {
                start != null -> start
                timeRangeDays != null -> endTime.minus(timeRangeDays.toLong(), ChronoUnit.DAYS)
                else -> endTime.minus(LOOKBACK_HOURS, ChronoUnit.HOURS)
            }

            if (startTime.isAfter(endTime)) {
                return Result.failure(IllegalArgumentException("start must be before or equal to end"))
            }

            val stepsData = if (HealthDataType.STEPS in enabledTypes)
                readStepsData(startTime, endTime, lastSyncTimestamps[HealthDataType.STEPS]) else emptyList()
            val sleepData = if (HealthDataType.SLEEP in enabledTypes)
                readSleepData(startTime, endTime, lastSyncTimestamps[HealthDataType.SLEEP]) else emptyList()
            val heartRateData = if (HealthDataType.HEART_RATE in enabledTypes)
                readHeartRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEART_RATE]) else emptyList()
            val heartRateVariabilityData = if (HealthDataType.HEART_RATE_VARIABILITY in enabledTypes)
                readHeartRateVariabilityData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEART_RATE_VARIABILITY]) else emptyList()
            val distanceData = if (HealthDataType.DISTANCE in enabledTypes)
                readDistanceData(startTime, endTime, lastSyncTimestamps[HealthDataType.DISTANCE]) else emptyList()
            val activeCaloriesData = if (HealthDataType.ACTIVE_CALORIES in enabledTypes)
                readActiveCaloriesData(startTime, endTime, lastSyncTimestamps[HealthDataType.ACTIVE_CALORIES]) else emptyList()
            val totalCaloriesData = if (HealthDataType.TOTAL_CALORIES in enabledTypes)
                readTotalCaloriesData(startTime, endTime, lastSyncTimestamps[HealthDataType.TOTAL_CALORIES]) else emptyList()
            val weightData = if (HealthDataType.WEIGHT in enabledTypes)
                readWeightData(startTime, endTime, lastSyncTimestamps[HealthDataType.WEIGHT]) else emptyList()
            val heightData = if (HealthDataType.HEIGHT in enabledTypes)
                readHeightData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEIGHT]) else emptyList()
            val bloodPressureData = if (HealthDataType.BLOOD_PRESSURE in enabledTypes)
                readBloodPressureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BLOOD_PRESSURE]) else emptyList()
            val bloodGlucoseData = if (HealthDataType.BLOOD_GLUCOSE in enabledTypes)
                readBloodGlucoseData(startTime, endTime, lastSyncTimestamps[HealthDataType.BLOOD_GLUCOSE]) else emptyList()
            val oxygenSaturationData = if (HealthDataType.OXYGEN_SATURATION in enabledTypes)
                readOxygenSaturationData(startTime, endTime, lastSyncTimestamps[HealthDataType.OXYGEN_SATURATION]) else emptyList()
            val bodyTemperatureData = if (HealthDataType.BODY_TEMPERATURE in enabledTypes)
                readBodyTemperatureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BODY_TEMPERATURE]) else emptyList()
            val respiratoryRateData = if (HealthDataType.RESPIRATORY_RATE in enabledTypes)
                readRespiratoryRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.RESPIRATORY_RATE]) else emptyList()
            val restingHeartRateData = if (HealthDataType.RESTING_HEART_RATE in enabledTypes)
                readRestingHeartRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.RESTING_HEART_RATE]) else emptyList()
            val exerciseData = if (HealthDataType.EXERCISE in enabledTypes)
                readExerciseData(
                    startTime,
                    endTime,
                    lastSyncTimestamps[HealthDataType.EXERCISE],
                    HealthDataType.DISTANCE in enabledTypes
                ) else emptyList()
            val hydrationData = if (HealthDataType.HYDRATION in enabledTypes)
                readHydrationData(startTime, endTime, lastSyncTimestamps[HealthDataType.HYDRATION]) else emptyList()
            val nutritionData = if (HealthDataType.NUTRITION in enabledTypes)
                readNutritionData(startTime, endTime, lastSyncTimestamps[HealthDataType.NUTRITION]) else emptyList()
            val basalMetabolicRateData = if (HealthDataType.BASAL_METABOLIC_RATE in enabledTypes)
                readBasalMetabolicRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.BASAL_METABOLIC_RATE]) else emptyList()
            val bodyFatData = if (HealthDataType.BODY_FAT in enabledTypes)
                readBodyFatData(startTime, endTime, lastSyncTimestamps[HealthDataType.BODY_FAT]) else emptyList()
            val leanBodyMassData = if (HealthDataType.LEAN_BODY_MASS in enabledTypes)
                readLeanBodyMassData(startTime, endTime, lastSyncTimestamps[HealthDataType.LEAN_BODY_MASS]) else emptyList()
            val vo2MaxData = if (HealthDataType.VO2_MAX in enabledTypes)
                readVo2MaxData(startTime, endTime, lastSyncTimestamps[HealthDataType.VO2_MAX]) else emptyList()
            val boneMassData = if (HealthDataType.BONE_MASS in enabledTypes)
                readBoneMassData(startTime, endTime, lastSyncTimestamps[HealthDataType.BONE_MASS]) else emptyList()

            Result.success(HealthData(
                steps = stepsData,
                sleep = sleepData,
                heartRate = heartRateData,
                heartRateVariability = heartRateVariabilityData,
                distance = distanceData,
                activeCalories = activeCaloriesData,
                totalCalories = totalCaloriesData,
                weight = weightData,
                height = heightData,
                bloodPressure = bloodPressureData,
                bloodGlucose = bloodGlucoseData,
                oxygenSaturation = oxygenSaturationData,
                bodyTemperature = bodyTemperatureData,
                respiratoryRate = respiratoryRateData,
                restingHeartRate = restingHeartRateData,
                exercise = exerciseData,
                hydration = hydrationData,
                nutrition = nutritionData,
                basalMetabolicRate = basalMetabolicRateData,
                bodyFat = bodyFatData,
                leanBodyMass = leanBodyMassData,
                vo2Max = vo2MaxData,
                boneMass = boneMassData
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun readStepsData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<StepsData> {
        // Aggregate steps per calendar day (using device timezone) instead of
        // a single multi-day total. This produces clean per-day records that
        // the server can use directly without delta-tracking hacks.
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<StepsData>()

        val startLocalDate = startTime.atZone(zone).toLocalDate()
        val endLocalDate = endTime.atZone(zone).toLocalDate()

        var currentDate = startLocalDate
        while (!currentDate.isAfter(endLocalDate)) {
            val dayStart = currentDate.atStartOfDay(zone).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            // Clamp to the actual lookback window
            val queryStart = if (dayStart.isBefore(startTime)) startTime else dayStart
            val queryEnd = if (dayEnd.isAfter(endTime)) endTime else dayEnd

            // Skip days entirely before lastSync
            if (lastSync != null && queryEnd.isBefore(lastSync)) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val request = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val response = healthConnectClient.aggregate(request)
            val daySteps = response[StepsRecord.COUNT_TOTAL] ?: 0L

            if (daySteps > 0) {
                result.add(StepsData(
                    count = daySteps,
                    startTime = dayStart,
                    endTime = queryEnd
                ))
            }

            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private suspend fun readSleepData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<SleepData> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )

        val response = healthConnectClient.readRecords(request)

        return response.records
            .filter { record ->
                lastSync == null || record.endTime >= lastSync
            }
            .map { record ->
                val stages = record.stages?.map { stage ->
                    SleepStage(
                        stage = stage.stage.toString(),
                        startTime = stage.startTime,
                        endTime = stage.endTime,
                        duration = Duration.between(stage.startTime, stage.endTime)
                    )
                } ?: emptyList()

                SleepData(
                    sessionEndTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    stages = stages
                )
            }
    }

    private suspend fun readHeartRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<HeartRateData> {
        val request = ReadRecordsRequest(recordType = HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records
            .flatMap { record ->
                record.samples
                    .filter { lastSync == null || it.time >= lastSync }
                    .map { HeartRateData(it.beatsPerMinute, it.time) }
            }
    }

    private suspend fun readHeartRateVariabilityData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<HeartRateVariabilityData> {
        val request = ReadRecordsRequest(recordType = HeartRateVariabilityRmssdRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { HeartRateVariabilityData(it.heartRateVariabilityMillis, it.time) }
    }

    private suspend fun readDistanceData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<DistanceData> {
        // Aggregate distance per calendar day (same pattern as steps)
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<DistanceData>()

        val startLocalDate = startTime.atZone(zone).toLocalDate()
        val endLocalDate = endTime.atZone(zone).toLocalDate()

        var currentDate = startLocalDate
        while (!currentDate.isAfter(endLocalDate)) {
            val dayStart = currentDate.atStartOfDay(zone).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            val queryStart = if (dayStart.isBefore(startTime)) startTime else dayStart
            val queryEnd = if (dayEnd.isAfter(endTime)) endTime else dayEnd

            if (lastSync != null && queryEnd.isBefore(lastSync)) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val request = AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val response = healthConnectClient.aggregate(request)
            val dayDistance = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0

            if (dayDistance > 0.0) {
                result.add(DistanceData(
                    meters = dayDistance,
                    startTime = dayStart,
                    endTime = queryEnd
                ))
            }

            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private suspend fun readActiveCaloriesData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<ActiveCaloriesData> {
        // Aggregate active calories per calendar day (same pattern as steps/distance)
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<ActiveCaloriesData>()

        val startLocalDate = startTime.atZone(zone).toLocalDate()
        val endLocalDate = endTime.atZone(zone).toLocalDate()

        var currentDate = startLocalDate
        while (!currentDate.isAfter(endLocalDate)) {
            val dayStart = currentDate.atStartOfDay(zone).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(zone).toInstant()

            val queryStart = if (dayStart.isBefore(startTime)) startTime else dayStart
            val queryEnd = if (dayEnd.isAfter(endTime)) endTime else dayEnd

            if (lastSync != null && queryEnd.isBefore(lastSync)) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val request = AggregateRequest(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val response = healthConnectClient.aggregate(request)
            val dayCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0

            if (dayCalories > 0.0) {
                result.add(ActiveCaloriesData(
                    calories = dayCalories,
                    startTime = dayStart,
                    endTime = queryEnd
                ))
            }

            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private suspend fun readTotalCaloriesData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<TotalCaloriesData> {
        val request = ReadRecordsRequest(recordType = TotalCaloriesBurnedRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map { TotalCaloriesData(it.energy.inKilocalories, it.startTime, it.endTime) }
    }

    private suspend fun readWeightData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<WeightData> {
        val request = ReadRecordsRequest(recordType = WeightRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { WeightData(it.weight.inKilograms, it.time) }
    }

    private suspend fun readHeightData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<HeightData> {
        val request = ReadRecordsRequest(recordType = HeightRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { HeightData(it.height.inMeters, it.time) }
    }

    private suspend fun readBloodPressureData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BloodPressureData> {
        val request = ReadRecordsRequest(recordType = BloodPressureRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BloodPressureData(it.systolic.inMillimetersOfMercury, it.diastolic.inMillimetersOfMercury, it.time) }
    }

    private suspend fun readBloodGlucoseData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BloodGlucoseData> {
        val request = ReadRecordsRequest(recordType = BloodGlucoseRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BloodGlucoseData(it.level.inMillimolesPerLiter, it.time) }
    }

    private suspend fun readOxygenSaturationData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<OxygenSaturationData> {
        val request = ReadRecordsRequest(recordType = OxygenSaturationRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { OxygenSaturationData(it.percentage.value, it.time) }
    }

    private suspend fun readBodyTemperatureData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BodyTemperatureData> {
        val request = ReadRecordsRequest(recordType = BodyTemperatureRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BodyTemperatureData(it.temperature.inCelsius, it.time) }
    }

    private suspend fun readRespiratoryRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<RespiratoryRateData> {
        val request = ReadRecordsRequest(recordType = RespiratoryRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { RespiratoryRateData(it.rate, it.time) }
    }

    private suspend fun readRestingHeartRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<RestingHeartRateData> {
        val request = ReadRecordsRequest(recordType = RestingHeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { RestingHeartRateData(it.beatsPerMinute, it.time) }
    }

    private suspend fun readExerciseData(startTime: Instant, endTime: Instant, lastSync: Instant?, includeDistance: Boolean): List<ExerciseData> {
        val request = ReadRecordsRequest(recordType = ExerciseSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map {
                ExerciseData(
                    type = it.exerciseType.toString(),
                    startTime = it.startTime,
                    endTime = it.endTime,
                    duration = Duration.between(it.startTime, it.endTime),
                    distanceMeters = if (includeDistance) readDistanceTotal(it.startTime, it.endTime) else null
                )
            }
    }

    private suspend fun readDistanceTotal(startTime: Instant, endTime: Instant): Double? {
        val request = AggregateRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val response = healthConnectClient.aggregate(request)
        val distanceMeters = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: return null
        return distanceMeters.takeIf { it > 0.0 }
    }

    private suspend fun readHydrationData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<HydrationData> {
        val request = ReadRecordsRequest(recordType = HydrationRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map { HydrationData(it.volume.inLiters, it.startTime, it.endTime) }
    }

    private suspend fun readNutritionData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<NutritionData> {
        val request = ReadRecordsRequest(recordType = NutritionRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map { NutritionData(it.energy?.inKilocalories, it.protein?.inGrams, it.totalCarbohydrate?.inGrams, it.totalFat?.inGrams, it.startTime, it.endTime) }
    }

    private suspend fun readBasalMetabolicRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BasalMetabolicRateData> {
        val request = ReadRecordsRequest(recordType = BasalMetabolicRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BasalMetabolicRateData(it.basalMetabolicRate.inWatts, it.time) }
    }

    private suspend fun readBodyFatData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BodyFatData> {
        val request = ReadRecordsRequest(recordType = BodyFatRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BodyFatData(it.percentage.value, it.time) }
    }

    private suspend fun readLeanBodyMassData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<LeanBodyMassData> {
        val request = ReadRecordsRequest(recordType = LeanBodyMassRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { LeanBodyMassData(it.mass.inKilograms, it.time) }
    }

    private suspend fun readVo2MaxData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<Vo2MaxData> {
        val request = ReadRecordsRequest(recordType = Vo2MaxRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { Vo2MaxData(it.vo2MillilitersPerMinuteKilogram, it.time) }
    }

    private suspend fun readBoneMassData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BoneMassData> {
        val request = ReadRecordsRequest(recordType = BoneMassRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BoneMassData(it.mass.inKilograms, it.time) }
    }

    fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasPermissions(requiredPermissions: Set<String> = ALL_PERMISSIONS): Boolean {
        if (!isHealthConnectAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        if (!isHealthConnectAvailable()) return emptySet()
        return healthConnectClient.permissionController.getGrantedPermissions()
    }

    suspend fun requestPermissions(permissions: Set<String>): android.content.Intent {
        if (!isHealthConnectAvailable()) {
            throw IllegalStateException("Health Connect is not available on this device")
        }
        val contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        return contract.createIntent(context, permissions.toTypedArray())
    }

    companion object {
        private const val LOOKBACK_HOURS = 48L

        fun getPermissionsForTypes(
            types: Set<HealthDataType>,
            includeBackgroundPermission: Boolean = true
        ): Set<String> {
            val permissions = types.map { HealthPermission.getReadPermission(it.recordClass) }.toMutableSet()
            if (includeBackgroundPermission) {
                permissions.add(BACKGROUND_PERMISSION_STR)
            }
            permissions.add("android.permission.health.READ_HEALTH_DATA_HISTORY")
            return permissions
        }

        const val BACKGROUND_PERMISSION_STR = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        val INITIAL_PERMISSIONS: Set<String>
            get() = ALL_PERMISSIONS + BACKGROUND_PERMISSION_STR

        val ALL_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(BloodGlucoseRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(LeanBodyMassRecord::class),
            HealthPermission.getReadPermission(Vo2MaxRecord::class),
            HealthPermission.getReadPermission(BoneMassRecord::class),
            "android.permission.health.READ_HEALTH_DATA_HISTORY"
        )
    }
}
