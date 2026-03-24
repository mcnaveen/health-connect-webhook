package com.hcwebhook.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume
import androidx.health.connect.client.records.MealType
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

enum class HealthDataType(val displayName: String, val recordClass: KClass<out Record>) {
    STEPS("Steps", StepsRecord::class),
    SLEEP("Sleep", SleepSessionRecord::class),
    HEART_RATE("Heart Rate", HeartRateRecord::class),
    HEART_RATE_VARIABILITY("Heart Rate Variability", HeartRateVariabilityRmssdRecord::class),
    DISTANCE("Distance", DistanceRecord::class),
    ACTIVE_CALORIES("Active Calories", ActiveCaloriesBurnedRecord::class),
    TOTAL_CALORIES("Total Calories", TotalCaloriesBurnedRecord::class),
    WEIGHT("Weight", WeightRecord::class),
    HEIGHT("Height", HeightRecord::class),
    BLOOD_PRESSURE("Blood Pressure", BloodPressureRecord::class),
    BLOOD_GLUCOSE("Blood Glucose", BloodGlucoseRecord::class),
    OXYGEN_SATURATION("Oxygen Saturation", OxygenSaturationRecord::class),
    BODY_TEMPERATURE("Body Temperature", BodyTemperatureRecord::class),
    RESPIRATORY_RATE("Respiratory Rate", RespiratoryRateRecord::class),
    RESTING_HEART_RATE("Resting Heart Rate", RestingHeartRateRecord::class),
    EXERCISE("Exercise Sessions", ExerciseSessionRecord::class),
    HYDRATION("Hydration", HydrationRecord::class),
    NUTRITION("Nutrition", NutritionRecord::class),
    SPEED("Speed", SpeedRecord::class),
    POWER("Power", PowerRecord::class),
    BODY_FAT("Body Fat", BodyFatRecord::class),
    BONE_MASS("Bone Mass", BoneMassRecord::class),
    LEAN_BODY_MASS("Lean Body Mass", LeanBodyMassRecord::class),
    MENSTRUATION("Menstruation", MenstruationPeriodRecord::class),
    VO2_MAX("VO2 Max", Vo2MaxRecord::class),
    FLOORS_CLIMBED("Floors Climbed", FloorsClimbedRecord::class),
    BASAL_METABOLIC_RATE("Basal Metabolic Rate", BasalMetabolicRateRecord::class)
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
    val speed: List<SpeedData>,
    val power: List<PowerData>,
    val bodyFat: List<BodyFatData>,
    val boneMass: List<BoneMassData>,
    val leanBodyMass: List<LeanBodyMassData>,
    val menstruation: List<MenstruationData>,
    val vo2Max: List<Vo2MaxData>,
    val floorsClimbed: List<FloorsClimbedData>,
    val basalMetabolicRate: List<BasalMetabolicRateData>
)

data class StepsData(
    val count: Long,
    val startTime: Instant,
    val endTime: Instant
)

data class SleepData(
    val sessionEndTime: Instant,
    val duration: Duration,
    val stages: List<SleepStage>,
    val awakeDuration: Duration,
    val lightDuration: Duration,
    val deepDuration: Duration,
    val remDuration: Duration
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
    val typeName: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val heartRateSamples: List<HeartRateData>
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

data class SpeedData(
    val metersPerSecond: Double,
    val time: Instant
)

data class PowerData(
    val watts: Double,
    val time: Instant
)

data class BodyFatData(
    val percentage: Double,
    val time: Instant
)

data class BoneMassData(
    val kilograms: Double,
    val time: Instant
)

data class LeanBodyMassData(
    val kilograms: Double,
    val time: Instant
)

data class MenstruationData(
    val startTime: Instant,
    val endTime: Instant
)

data class Vo2MaxData(
    val vo2MillilitersPerMinuteKilogram: Double,
    val time: Instant
)

data class FloorsClimbedData(
    val floors: Double,
    val startTime: Instant,
    val endTime: Instant
)

data class BasalMetabolicRateData(
    val kcalPerDay: Double,
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
        timeRangeDays: Int? = null
    ): Result<HealthData> {
        return try {
            val endTime = Instant.now()
            val startTime = if (timeRangeDays != null) {
                endTime.minus(timeRangeDays.toLong(), ChronoUnit.DAYS)
            } else {
                endTime.minus(LOOKBACK_HOURS, ChronoUnit.HOURS)
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
                readExerciseData(startTime, endTime, lastSyncTimestamps[HealthDataType.EXERCISE]) else emptyList()
            val hydrationData = if (HealthDataType.HYDRATION in enabledTypes)
                readHydrationData(startTime, endTime, lastSyncTimestamps[HealthDataType.HYDRATION]) else emptyList()
            val nutritionData = if (HealthDataType.NUTRITION in enabledTypes)
                readNutritionData(startTime, endTime, lastSyncTimestamps[HealthDataType.NUTRITION]) else emptyList()
            val speedData = if (HealthDataType.SPEED in enabledTypes)
                readSpeedData(startTime, endTime, lastSyncTimestamps[HealthDataType.SPEED]) else emptyList()
            val powerData = if (HealthDataType.POWER in enabledTypes)
                readPowerData(startTime, endTime, lastSyncTimestamps[HealthDataType.POWER]) else emptyList()
            val bodyFatData = if (HealthDataType.BODY_FAT in enabledTypes)
                readBodyFatData(startTime, endTime, lastSyncTimestamps[HealthDataType.BODY_FAT]) else emptyList()
            val boneMassData = if (HealthDataType.BONE_MASS in enabledTypes)
                readBoneMassData(startTime, endTime, lastSyncTimestamps[HealthDataType.BONE_MASS]) else emptyList()
            val leanBodyMassData = if (HealthDataType.LEAN_BODY_MASS in enabledTypes)
                readLeanBodyMassData(startTime, endTime, lastSyncTimestamps[HealthDataType.LEAN_BODY_MASS]) else emptyList()
            val menstruationData = if (HealthDataType.MENSTRUATION in enabledTypes)
                readMenstruationData(startTime, endTime, lastSyncTimestamps[HealthDataType.MENSTRUATION]) else emptyList()
            val vo2MaxData = if (HealthDataType.VO2_MAX in enabledTypes)
                readVo2MaxData(startTime, endTime, lastSyncTimestamps[HealthDataType.VO2_MAX]) else emptyList()
            val floorsClimbedData = if (HealthDataType.FLOORS_CLIMBED in enabledTypes)
                readFloorsClimbedData(startTime, endTime, lastSyncTimestamps[HealthDataType.FLOORS_CLIMBED]) else emptyList()
            val basalMetabolicRateData = if (HealthDataType.BASAL_METABOLIC_RATE in enabledTypes)
                readBasalMetabolicRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.BASAL_METABOLIC_RATE]) else emptyList()

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
                speed = speedData,
                power = powerData,
                bodyFat = bodyFatData,
                boneMass = boneMassData,
                leanBodyMass = leanBodyMassData,
                menstruation = menstruationData,
                vo2Max = vo2MaxData,
                floorsClimbed = floorsClimbedData,
                basalMetabolicRate = basalMetabolicRateData
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
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<StepsData>()

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
                        stage = getSleepStageName(stage.stage),
                        startTime = stage.startTime,
                        endTime = stage.endTime,
                        duration = Duration.between(stage.startTime, stage.endTime)
                    )
                } ?: emptyList()

                val awakeDuration = stages.filter { it.stage == "AWAKE" }
                    .fold(Duration.ZERO) { acc, s -> acc + s.duration }
                val lightDuration = stages.filter { it.stage == "LIGHT" }
                    .fold(Duration.ZERO) { acc, s -> acc + s.duration }
                val deepDuration = stages.filter { it.stage == "DEEP" }
                    .fold(Duration.ZERO) { acc, s -> acc + s.duration }
                val remDuration = stages.filter { it.stage == "REM" }
                    .fold(Duration.ZERO) { acc, s -> acc + s.duration }

                SleepData(
                    sessionEndTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    stages = stages,
                    awakeDuration = awakeDuration,
                    lightDuration = lightDuration,
                    deepDuration = deepDuration,
                    remDuration = remDuration
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

    private suspend fun readExerciseData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<ExerciseData> {
        val request = ReadRecordsRequest(recordType = ExerciseSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map { record ->
                val heartRateSamples = try {
                    val hrRequest = ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                    )
                    val hrResponse = healthConnectClient.readRecords(hrRequest)
                    hrResponse.records.flatMap { hr ->
                        hr.samples.map { HeartRateData(it.beatsPerMinute, it.time) }
                    }
                } catch (_: Exception) {
                    emptyList()
                }

                ExerciseData(
                    type = record.exerciseType.toString(),
                    typeName = getExerciseTypeName(record.exerciseType),
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    heartRateSamples = heartRateSamples
                )
            }
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

    private suspend fun readSpeedData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<SpeedData> {
        val request = ReadRecordsRequest(recordType = SpeedRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.flatMap { record ->
            record.samples
                .filter { lastSync == null || it.time >= lastSync }
                .map { SpeedData(it.speed.inMetersPerSecond, it.time) }
        }
    }

    private suspend fun readPowerData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<PowerData> {
        val request = ReadRecordsRequest(recordType = PowerRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.flatMap { record ->
            record.samples
                .filter { lastSync == null || it.time >= lastSync }
                .map { PowerData(it.power.inWatts, it.time) }
        }
    }

    private suspend fun readBodyFatData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BodyFatData> {
        val request = ReadRecordsRequest(recordType = BodyFatRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BodyFatData(it.percentage.value, it.time) }
    }

    private suspend fun readBoneMassData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BoneMassData> {
        val request = ReadRecordsRequest(recordType = BoneMassRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BoneMassData(it.mass.inKilograms, it.time) }
    }

    private suspend fun readLeanBodyMassData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<LeanBodyMassData> {
        val request = ReadRecordsRequest(recordType = LeanBodyMassRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { LeanBodyMassData(it.mass.inKilograms, it.time) }
    }

    private suspend fun readMenstruationData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<MenstruationData> {
        val request = ReadRecordsRequest(recordType = MenstruationPeriodRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map { MenstruationData(it.startTime, it.endTime) }
    }

    private suspend fun readVo2MaxData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<Vo2MaxData> {
        val request = ReadRecordsRequest(recordType = Vo2MaxRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { Vo2MaxData(it.vo2MillilitersPerMinuteKilogram, it.time) }
    }

    private suspend fun readFloorsClimbedData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<FloorsClimbedData> {
        val request = ReadRecordsRequest(recordType = FloorsClimbedRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.endTime >= lastSync }
            .map { FloorsClimbedData(it.floors, it.startTime, it.endTime) }
    }

    private suspend fun readBasalMetabolicRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BasalMetabolicRateData> {
        val request = ReadRecordsRequest(recordType = BasalMetabolicRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = healthConnectClient.readRecords(request)
        return response.records.filter { lastSync == null || it.time >= lastSync }
            .map { BasalMetabolicRateData(it.basalMetabolicRate.inKilocaloriesPerDay, it.time) }
    }

    suspend fun insertNutrition(
        calories: Double?,
        protein: Double?,
        carbs: Double?,
        fat: Double?,
        startTime: Instant,
        endTime: Instant,
        name: String? = null,
        mealType: Int = MealType.MEAL_TYPE_UNKNOWN,
        saturatedFat: Double? = null,
        monounsaturatedFat: Double? = null,
        polyunsaturatedFat: Double? = null,
        transFat: Double? = null,
        dietaryFiber: Double? = null,
        sugar: Double? = null,
        cholesterol: Double? = null,
        caffeine: Double? = null,
        vitaminA: Double? = null,
        vitaminB6: Double? = null,
        vitaminB12: Double? = null,
        vitaminC: Double? = null,
        vitaminD: Double? = null,
        vitaminE: Double? = null,
        vitaminK: Double? = null,
        biotin: Double? = null,
        folate: Double? = null,
        folicAcid: Double? = null,
        niacin: Double? = null,
        pantothenicAcid: Double? = null,
        riboflavin: Double? = null,
        thiamin: Double? = null,
        calcium: Double? = null,
        iron: Double? = null,
        magnesium: Double? = null,
        zinc: Double? = null,
        potassium: Double? = null,
        sodium: Double? = null,
        phosphorus: Double? = null,
        manganese: Double? = null,
        copper: Double? = null,
        selenium: Double? = null,
        chromium: Double? = null,
        iodine: Double? = null,
        molybdenum: Double? = null,
        chloride: Double? = null
    ): Result<Unit> {
        return try {
            val record = NutritionRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                name = name,
                mealType = mealType,
                energy = calories?.let { Energy.kilocalories(it) },
                protein = protein?.let { Mass.grams(it) },
                totalCarbohydrate = carbs?.let { Mass.grams(it) },
                totalFat = fat?.let { Mass.grams(it) },
                saturatedFat = saturatedFat?.let { Mass.grams(it) },
                monounsaturatedFat = monounsaturatedFat?.let { Mass.grams(it) },
                polyunsaturatedFat = polyunsaturatedFat?.let { Mass.grams(it) },
                transFat = transFat?.let { Mass.grams(it) },
                dietaryFiber = dietaryFiber?.let { Mass.grams(it) },
                sugar = sugar?.let { Mass.grams(it) },
                cholesterol = cholesterol?.let { Mass.milligrams(it) },
                caffeine = caffeine?.let { Mass.milligrams(it) },
                vitaminA = vitaminA?.let { Mass.micrograms(it) },
                vitaminB6 = vitaminB6?.let { Mass.milligrams(it) },
                vitaminB12 = vitaminB12?.let { Mass.micrograms(it) },
                vitaminC = vitaminC?.let { Mass.milligrams(it) },
                vitaminD = vitaminD?.let { Mass.micrograms(it) },
                vitaminE = vitaminE?.let { Mass.milligrams(it) },
                vitaminK = vitaminK?.let { Mass.micrograms(it) },
                biotin = biotin?.let { Mass.micrograms(it) },
                folate = folate?.let { Mass.micrograms(it) },
                folicAcid = folicAcid?.let { Mass.micrograms(it) },
                niacin = niacin?.let { Mass.milligrams(it) },
                pantothenicAcid = pantothenicAcid?.let { Mass.milligrams(it) },
                riboflavin = riboflavin?.let { Mass.milligrams(it) },
                thiamin = thiamin?.let { Mass.milligrams(it) },
                calcium = calcium?.let { Mass.milligrams(it) },
                iron = iron?.let { Mass.milligrams(it) },
                magnesium = magnesium?.let { Mass.milligrams(it) },
                zinc = zinc?.let { Mass.milligrams(it) },
                potassium = potassium?.let { Mass.milligrams(it) },
                sodium = sodium?.let { Mass.milligrams(it) },
                phosphorus = phosphorus?.let { Mass.milligrams(it) },
                manganese = manganese?.let { Mass.milligrams(it) },
                copper = copper?.let { Mass.milligrams(it) },
                selenium = selenium?.let { Mass.micrograms(it) },
                chromium = chromium?.let { Mass.micrograms(it) },
                iodine = iodine?.let { Mass.micrograms(it) },
                molybdenum = molybdenum?.let { Mass.micrograms(it) },
                chloride = chloride?.let { Mass.milligrams(it) }
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertHydration(
        liters: Double,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = HydrationRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                volume = Volume.liters(liters)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertWeight(
        kilograms: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = WeightRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                weight = Mass.kilograms(kilograms)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertSteps(
        count: Long,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = StepsRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                count = count
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertHeartRate(
        bpm: Long,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = HeartRateRecord(
                startTime = time,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                endTime = time.plusSeconds(1),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                samples = listOf(HeartRateRecord.Sample(time, bpm))
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertSleep(
        startTime: Instant,
        endTime: Instant,
        stages: List<SleepSessionRecord.Stage>,
        title: String? = null,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val record = SleepSessionRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                stages = stages,
                title = title,
                notes = notes
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertDistance(
        meters: Double,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = DistanceRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                distance = Length.meters(meters)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertActiveCalories(
        calories: Double,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = ActiveCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                energy = Energy.kilocalories(calories)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertTotalCalories(
        calories: Double,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = TotalCaloriesBurnedRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                energy = Energy.kilocalories(calories)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertHeight(
        meters: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = HeightRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                height = Length.meters(meters)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertOxygenSaturation(
        percentage: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = OxygenSaturationRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                percentage = Percentage(percentage)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertHeartRateVariability(
        milliseconds: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = HeartRateVariabilityRmssdRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                heartRateVariabilityMillis = milliseconds
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertBasalMetabolicRate(
        kcalPerDay: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = BasalMetabolicRateRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                basalMetabolicRate = Power.kilocaloriesPerDay(kcalPerDay)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertBodyFat(
        percentage: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = BodyFatRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                percentage = Percentage(percentage)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertLeanBodyMass(
        kilograms: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = LeanBodyMassRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                mass = Mass.kilograms(kilograms)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertRestingHeartRate(
        bpm: Long,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = RestingHeartRateRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                beatsPerMinute = bpm
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertVo2Max(
        vo2MillilitersPerMinuteKilogram: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = Vo2MaxRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                vo2MillilitersPerMinuteKilogram = vo2MillilitersPerMinuteKilogram
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertBoneMass(
        kilograms: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = BoneMassRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                mass = Mass.kilograms(kilograms)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertBloodPressure(
        systolic: Double,
        diastolic: Double,
        time: Instant,
        bodyPosition: Int = BloodPressureRecord.BODY_POSITION_UNKNOWN,
        measurementLocation: Int = BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN
    ): Result<Unit> {
        return try {
            val record = BloodPressureRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                systolic = Pressure.millimetersOfMercury(systolic),
                diastolic = Pressure.millimetersOfMercury(diastolic),
                bodyPosition = bodyPosition,
                measurementLocation = measurementLocation
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertBloodGlucose(
        millimolePerLiter: Double,
        time: Instant,
        specimenSource: Int = BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN,
        mealType: Int = MealType.MEAL_TYPE_UNKNOWN,
        relationToMeal: Int = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN
    ): Result<Unit> {
        return try {
            val record = BloodGlucoseRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                level = BloodGlucose.millimolesPerLiter(millimolePerLiter),
                specimenSource = specimenSource,
                mealType = mealType,
                relationToMeal = relationToMeal
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertBodyTemperature(
        celsius: Double,
        time: Instant,
        measurementLocation: Int = 0
    ): Result<Unit> {
        return try {
            val record = BodyTemperatureRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                temperature = Temperature.celsius(celsius),
                measurementLocation = measurementLocation
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertRespiratoryRate(
        rate: Double,
        time: Instant
    ): Result<Unit> {
        return try {
            val record = RespiratoryRateRecord(
                time = time,
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(time),
                rate = rate
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertExerciseSession(
        type: Int,
        startTime: Instant,
        endTime: Instant,
        title: String?,
        notes: String? = null,
        laps: List<ExerciseLap> = emptyList(),
        segments: List<ExerciseSegment> = emptyList(),
        route: ExerciseRoute? = null
    ): Result<Unit> {
        return try {
            val record = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                exerciseType = type,
                title = title,
                notes = notes,
                laps = laps,
                segments = segments,
                exerciseRoute = route
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertFloorsClimbed(
        floors: Double,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = FloorsClimbedRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                floors = floors
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertMenstruation(
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = MenstruationPeriodRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime)
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertSpeed(
        samples: List<Pair<Instant, Double>>,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = SpeedRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                samples = samples.map { (time, metersPerSecond) ->
                    SpeedRecord.Sample(
                        time = time,
                        speed = Velocity.metersPerSecond(metersPerSecond)
                    )
                }
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertPower(
        samples: List<Pair<Instant, Double>>,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit> {
        return try {
            val record = PowerRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                samples = samples.map { (time, watts) ->
                    PowerRecord.Sample(
                        time = time,
                        power = Power.watts(watts)
                    )
                }
            )
            healthConnectClient.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasPermissions(requiredPermissions: Set<String> = READ_PERMISSIONS): Boolean {
        if (!isHealthConnectAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    suspend fun hasAnyWritePermissions(): Boolean {
        if (!isHealthConnectAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return CORE_WRITE_PERMISSIONS.any { it in granted }
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

        fun getPermissionsForTypes(types: Set<HealthDataType>): Set<String> {
            val permissions = types.map { HealthPermission.getReadPermission(it.recordClass) }.toMutableSet()
            permissions.add("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND")
            return permissions
        }

        val READ_PERMISSIONS = setOf(
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
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(BoneMassRecord::class),
            HealthPermission.getReadPermission(LeanBodyMassRecord::class),
            HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
            HealthPermission.getReadPermission(Vo2MaxRecord::class),
            HealthPermission.getReadPermission(FloorsClimbedRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        )

        val WRITE_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getWritePermission(HeightRecord::class),
            HealthPermission.getWritePermission(BloodPressureRecord::class),
            HealthPermission.getWritePermission(BloodGlucoseRecord::class),
            HealthPermission.getWritePermission(OxygenSaturationRecord::class),
            HealthPermission.getWritePermission(BodyTemperatureRecord::class),
            HealthPermission.getWritePermission(RespiratoryRateRecord::class),
            HealthPermission.getWritePermission(RestingHeartRateRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class),
            HealthPermission.getWritePermission(SpeedRecord::class),
            HealthPermission.getWritePermission(PowerRecord::class),
            HealthPermission.getWritePermission(BodyFatRecord::class),
            HealthPermission.getWritePermission(BoneMassRecord::class),
            HealthPermission.getWritePermission(LeanBodyMassRecord::class),
            HealthPermission.getWritePermission(MenstruationPeriodRecord::class),
            HealthPermission.getWritePermission(Vo2MaxRecord::class),
            HealthPermission.getWritePermission(FloorsClimbedRecord::class),
            HealthPermission.getWritePermission(BasalMetabolicRateRecord::class)
        )

        val CORE_WRITE_PERMISSIONS = setOf(
            HealthPermission.getWritePermission(NutritionRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class)
        )

        val ALL_PERMISSIONS = READ_PERMISSIONS + WRITE_PERMISSIONS

        private fun getSleepStageName(stage: Int): String = when (stage) {
            SleepSessionRecord.STAGE_TYPE_AWAKE -> "AWAKE"
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> "SLEEPING"
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "OUT_OF_BED"
            SleepSessionRecord.STAGE_TYPE_LIGHT -> "LIGHT"
            SleepSessionRecord.STAGE_TYPE_DEEP -> "DEEP"
            SleepSessionRecord.STAGE_TYPE_REM -> "REM"
            else -> "UNKNOWN"
        }

        private fun getExerciseTypeName(type: Int): String = when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "Badminton"
            ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> "Baseball"
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "Basketball"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Biking"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Stationary Biking"
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> "Boot Camp"
            ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "Boxing"
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
            ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> "Cricket"
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dancing"
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "Exercise Class"
            ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> "Fencing"
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> "American Football"
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> "Australian Football"
            ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> "Frisbee"
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> "Golf"
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> "Guided Breathing"
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "Gymnastics"
            ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> "Handball"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> "Ice Hockey"
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> "Ice Skating"
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "Martial Arts"
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "Paddling"
            ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING -> "Paragliding"
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Pilates"
            ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> "Racquetball"
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> "Rock Climbing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> "Roller Hockey"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "Rowing"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Rowing Machine"
            ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> "Rugby"
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Treadmill Running"
            ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> "Sailing"
            ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> "Scuba Diving"
            ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> "Skating"
            ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> "Skiing"
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> "Snowboarding"
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> "Snowshoeing"
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Soccer"
            ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> "Softball"
            ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> "Squash"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "Stair Climbing"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "Stair Machine"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "Stretching"
            ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> "Surfing"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Open Water Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Pool Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "Table Tennis"
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "Tennis"
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "Volleyball"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> "Water Polo"
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> "Wheelchair"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "Other Workout"
            else -> "Unknown ($type)"
        }
    }
}
