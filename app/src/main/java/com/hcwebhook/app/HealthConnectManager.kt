package com.hcwebhook.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.hcwebhook.app.dashboard.DashboardFormatter
import com.hcwebhook.app.dashboard.DashboardMetric
import com.hcwebhook.app.dashboard.DashboardSnapshot
import kotlin.math.roundToLong
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
    SKIN_TEMPERATURE(
        R.string.dt_skin_temp_name, SkinTemperatureRecord::class, R.string.dt_skin_temp_rationale
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
    ),
    MENSTRUATION_FLOW(
        R.string.dt_menstruation_flow_name, MenstruationFlowRecord::class, R.string.dt_menstruation_flow_rationale
    ),
    MENSTRUATION_PERIOD(
        R.string.dt_menstruation_period_name, MenstruationPeriodRecord::class, R.string.dt_menstruation_period_rationale
    ),
    INTERMENSTRUAL_BLEEDING(
        R.string.dt_intermenstrual_bleeding_name, IntermenstrualBleedingRecord::class, R.string.dt_intermenstrual_bleeding_rationale
    ),
    OVULATION_TEST(
        R.string.dt_ovulation_test_name, OvulationTestRecord::class, R.string.dt_ovulation_test_rationale
    ),
    CERVICAL_MUCUS(
        R.string.dt_cervical_mucus_name, CervicalMucusRecord::class, R.string.dt_cervical_mucus_rationale
    ),
    SEXUAL_ACTIVITY(
        R.string.dt_sexual_activity_name, SexualActivityRecord::class, R.string.dt_sexual_activity_rationale
    ),
    BASAL_BODY_TEMPERATURE(
        R.string.dt_basal_body_temp_name, BasalBodyTemperatureRecord::class, R.string.dt_basal_body_temp_rationale
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
    val skinTemperature: List<SkinTemperatureData>,
    val respiratoryRate: List<RespiratoryRateData>,
    val restingHeartRate: List<RestingHeartRateData>,
    val exercise: List<ExerciseData>,
    val hydration: List<HydrationData>,
    val nutrition: List<NutritionData>,
    val basalMetabolicRate: List<BasalMetabolicRateData>,
    val bodyFat: List<BodyFatData>,
    val leanBodyMass: List<LeanBodyMassData>,
    val vo2Max: List<Vo2MaxData>,
    val boneMass: List<BoneMassData>,
    val menstruationFlow: List<MenstruationFlowData>,
    val menstruationPeriod: List<MenstruationPeriodData>,
    val intermenstrualBleeding: List<IntermenstrualBleedingData>,
    val ovulationTest: List<OvulationTestData>,
    val cervicalMucus: List<CervicalMucusData>,
    val sexualActivity: List<SexualActivityData>,
    val basalBodyTemperature: List<BasalBodyTemperatureData>
)

/**
 * Provenance of a Health Connect record: which app wrote it, how it was recorded, and on what
 * device. Lets downstream consumers tell e.g. an automatically-detected session apart from one
 * recorded by gym equipment, and deduplicate records that arrive from multiple source apps.
 */
data class RecordMetadata(
    val dataOrigin: String,
    val recordingMethod: String,
    val deviceManufacturer: String? = null,
    val deviceModel: String? = null,
    val deviceType: Int? = null
)

fun Metadata.toRecordMetadata(): RecordMetadata = RecordMetadata(
    dataOrigin = dataOrigin.packageName,
    recordingMethod = when (recordingMethod) {
        Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> "actively_recorded"
        Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> "automatically_recorded"
        Metadata.RECORDING_METHOD_MANUAL_ENTRY -> "manual_entry"
        else -> "unknown"
    },
    deviceManufacturer = device?.manufacturer,
    deviceModel = device?.model,
    deviceType = device?.type,
)

data class StepsData(
    val count: Long,
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class SleepData(
    val sessionEndTime: Instant,
    val duration: Duration,
    val stages: List<SleepStage>,
    val metadata: RecordMetadata? = null
)

data class SleepStage(
    val stage: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration
)

data class HeartRateData(
    val bpm: Long,
    val time: Instant,
    val min: Long? = null,
    val max: Long? = null,
    val metadata: RecordMetadata? = null,
)

data class HeartRateVariabilityData(
    val rmssdMillis: Double,
    val time: Instant,
    val min: Double? = null,
    val max: Double? = null,
    val metadata: RecordMetadata? = null,
)

data class DistanceData(
    val meters: Double,
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class ActiveCaloriesData(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class TotalCaloriesData(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class WeightData(
    val kilograms: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class HeightData(
    val meters: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class BloodPressureData(
    val systolic: Double,
    val diastolic: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class BloodGlucoseData(
    val mmolPerLiter: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class OxygenSaturationData(
    val percentage: Double,
    val time: Instant,
    val min: Double? = null,
    val max: Double? = null,
    val metadata: RecordMetadata? = null,
)

data class BodyTemperatureData(
    val celsius: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class SkinTemperatureData(
    val time: Instant,
    val deltaCelsius: Double,
    val baselineCelsius: Double?,
    val measurementLocation: Int,
    val minDeltaCelsius: Double? = null,
    val maxDeltaCelsius: Double? = null,
    val metadata: RecordMetadata? = null,
)

data class RespiratoryRateData(
    val rate: Double,
    val time: Instant,
    val min: Double? = null,
    val max: Double? = null,
    val metadata: RecordMetadata? = null,
)

data class RestingHeartRateData(
    val bpm: Long,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class ExerciseData(
    val type: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val distanceMeters: Double? = null,
    val steps: Long? = null,
    val avgCadenceSpm: Double? = null,
    val maxCadenceSpm: Double? = null,
    val strideLengthMeters: Double? = null,
    val metadata: RecordMetadata? = null
)

data class HydrationData(
    val liters: Double,
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class NutritionData(
    val calories: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val sugar: Double?,
    val sodium: Double?,
    val dietaryFiber: Double?,
    val name: String?,
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class BasalMetabolicRateData(
    val watts: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class BodyFatData(
    val percentage: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class LeanBodyMassData(
    val kilograms: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class Vo2MaxData(
    val mlPerKgPerMin: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class BoneMassData(
    val kilograms: Double,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class MenstruationFlowData(
    val flow: Int,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class MenstruationPeriodData(
    val startTime: Instant,
    val endTime: Instant,
    val metadata: RecordMetadata? = null
)

data class IntermenstrualBleedingData(
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class OvulationTestData(
    val result: Int,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class CervicalMucusData(
    val appearance: Int,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class SexualActivityData(
    val protectionUsed: Int,
    val time: Instant,
    val metadata: RecordMetadata? = null
)

data class BasalBodyTemperatureData(
    val celsius: Double,
    val measurementLocation: Int,
    val time: Instant,
    val metadata: RecordMetadata? = null
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
        end: Instant? = null,
        dataTypeResolutions: Map<HealthDataType, Int> = emptyMap(),
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

            fun resolution(type: HealthDataType): Int =
                dataTypeResolutions[type] ?: type.defaultResolutionMinutes

            val stepsData = if (HealthDataType.STEPS in enabledTypes)
                readStepsData(startTime, endTime, lastSyncTimestamps[HealthDataType.STEPS], resolution(HealthDataType.STEPS)) else emptyList()
            val sleepData = if (HealthDataType.SLEEP in enabledTypes)
                readSleepData(startTime, endTime, lastSyncTimestamps[HealthDataType.SLEEP], resolution(HealthDataType.SLEEP)) else emptyList()
            val heartRateData = if (HealthDataType.HEART_RATE in enabledTypes)
                readHeartRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEART_RATE], resolution(HealthDataType.HEART_RATE)) else emptyList()
            val heartRateVariabilityData = if (HealthDataType.HEART_RATE_VARIABILITY in enabledTypes)
                readHeartRateVariabilityData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEART_RATE_VARIABILITY], resolution(HealthDataType.HEART_RATE_VARIABILITY)) else emptyList()
            val distanceData = if (HealthDataType.DISTANCE in enabledTypes)
                readDistanceData(startTime, endTime, lastSyncTimestamps[HealthDataType.DISTANCE], resolution(HealthDataType.DISTANCE)) else emptyList()
            val activeCaloriesData = if (HealthDataType.ACTIVE_CALORIES in enabledTypes)
                readActiveCaloriesData(startTime, endTime, lastSyncTimestamps[HealthDataType.ACTIVE_CALORIES], resolution(HealthDataType.ACTIVE_CALORIES)) else emptyList()
            val totalCaloriesData = if (HealthDataType.TOTAL_CALORIES in enabledTypes)
                readTotalCaloriesData(startTime, endTime, lastSyncTimestamps[HealthDataType.TOTAL_CALORIES], resolution(HealthDataType.TOTAL_CALORIES)) else emptyList()
            val weightData = if (HealthDataType.WEIGHT in enabledTypes)
                readWeightData(startTime, endTime, lastSyncTimestamps[HealthDataType.WEIGHT]) else emptyList()
            val heightData = if (HealthDataType.HEIGHT in enabledTypes)
                readHeightData(startTime, endTime, lastSyncTimestamps[HealthDataType.HEIGHT]) else emptyList()
            val bloodPressureData = if (HealthDataType.BLOOD_PRESSURE in enabledTypes)
                readBloodPressureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BLOOD_PRESSURE]) else emptyList()
            val bloodGlucoseData = if (HealthDataType.BLOOD_GLUCOSE in enabledTypes)
                readBloodGlucoseData(startTime, endTime, lastSyncTimestamps[HealthDataType.BLOOD_GLUCOSE]) else emptyList()
            val oxygenSaturationData = if (HealthDataType.OXYGEN_SATURATION in enabledTypes)
                readOxygenSaturationData(startTime, endTime, lastSyncTimestamps[HealthDataType.OXYGEN_SATURATION], resolution(HealthDataType.OXYGEN_SATURATION)) else emptyList()
            val bodyTemperatureData = if (HealthDataType.BODY_TEMPERATURE in enabledTypes)
                readBodyTemperatureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BODY_TEMPERATURE]) else emptyList()
            val skinTemperatureData = if (HealthDataType.SKIN_TEMPERATURE in enabledTypes)
                readSkinTemperatureData(startTime, endTime, lastSyncTimestamps[HealthDataType.SKIN_TEMPERATURE], resolution(HealthDataType.SKIN_TEMPERATURE)) else emptyList()
            val respiratoryRateData = if (HealthDataType.RESPIRATORY_RATE in enabledTypes)
                readRespiratoryRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.RESPIRATORY_RATE], resolution(HealthDataType.RESPIRATORY_RATE)) else emptyList()
            val restingHeartRateData = if (HealthDataType.RESTING_HEART_RATE in enabledTypes)
                readRestingHeartRateData(startTime, endTime, lastSyncTimestamps[HealthDataType.RESTING_HEART_RATE]) else emptyList()
            val exerciseData = if (HealthDataType.EXERCISE in enabledTypes)
                readExerciseData(
                    startTime,
                    endTime,
                    lastSyncTimestamps[HealthDataType.EXERCISE],
                    HealthDataType.DISTANCE in enabledTypes,
                    HealthDataType.STEPS in enabledTypes
                ) else emptyList()
            val hydrationData = if (HealthDataType.HYDRATION in enabledTypes)
                readHydrationData(startTime, endTime, lastSyncTimestamps[HealthDataType.HYDRATION], resolution(HealthDataType.HYDRATION)) else emptyList()
            val nutritionData = if (HealthDataType.NUTRITION in enabledTypes)
                readNutritionData(startTime, endTime, lastSyncTimestamps[HealthDataType.NUTRITION], resolution(HealthDataType.NUTRITION)) else emptyList()
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
            val menstruationFlowData = if (HealthDataType.MENSTRUATION_FLOW in enabledTypes)
                readMenstruationFlowData(startTime, endTime, lastSyncTimestamps[HealthDataType.MENSTRUATION_FLOW]) else emptyList()
            val menstruationPeriodData = if (HealthDataType.MENSTRUATION_PERIOD in enabledTypes)
                readMenstruationPeriodData(startTime, endTime, lastSyncTimestamps[HealthDataType.MENSTRUATION_PERIOD]) else emptyList()
            val intermenstrualBleedingData = if (HealthDataType.INTERMENSTRUAL_BLEEDING in enabledTypes)
                readIntermenstrualBleedingData(startTime, endTime, lastSyncTimestamps[HealthDataType.INTERMENSTRUAL_BLEEDING]) else emptyList()
            val ovulationTestData = if (HealthDataType.OVULATION_TEST in enabledTypes)
                readOvulationTestData(startTime, endTime, lastSyncTimestamps[HealthDataType.OVULATION_TEST]) else emptyList()
            val cervicalMucusData = if (HealthDataType.CERVICAL_MUCUS in enabledTypes)
                readCervicalMucusData(startTime, endTime, lastSyncTimestamps[HealthDataType.CERVICAL_MUCUS]) else emptyList()
            val sexualActivityData = if (HealthDataType.SEXUAL_ACTIVITY in enabledTypes)
                readSexualActivityData(startTime, endTime, lastSyncTimestamps[HealthDataType.SEXUAL_ACTIVITY]) else emptyList()
            val basalBodyTemperatureData = if (HealthDataType.BASAL_BODY_TEMPERATURE in enabledTypes)
                readBasalBodyTemperatureData(startTime, endTime, lastSyncTimestamps[HealthDataType.BASAL_BODY_TEMPERATURE]) else emptyList()

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
                skinTemperature = skinTemperatureData,
                respiratoryRate = respiratoryRateData,
                restingHeartRate = restingHeartRateData,
                exercise = exerciseData,
                hydration = hydrationData,
                nutrition = nutritionData,
                basalMetabolicRate = basalMetabolicRateData,
                bodyFat = bodyFatData,
                leanBodyMass = leanBodyMassData,
                vo2Max = vo2MaxData,
                boneMass = boneMassData,
                menstruationFlow = menstruationFlowData,
                menstruationPeriod = menstruationPeriodData,
                intermenstrualBleeding = intermenstrualBleedingData,
                ovulationTest = ovulationTestData,
                cervicalMucus = cervicalMucusData,
                sexualActivity = sexualActivityData,
                basalBodyTemperature = basalBodyTemperatureData
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readTodayDashboardStats(grantedPermissions: Set<String>): Result<DashboardSnapshot> {
        return try {
            val grantedTypes = grantedDataTypes(grantedPermissions)
            if (grantedTypes.isEmpty()) {
                return Result.success(DashboardSnapshot(emptyList()))
            }

            val zone = ZoneId.systemDefault()
            val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val dayEnd = Instant.now()

            val metrics = grantedTypes.map { type ->
                readDashboardMetric(type, dayStart, dayEnd)
            }

            Result.success(DashboardSnapshot(metrics, Instant.now()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun readDashboardMetric(
        type: HealthDataType,
        dayStart: Instant,
        dayEnd: Instant,
    ): DashboardMetric = when (type) {
        HealthDataType.STEPS -> DashboardMetric(
            type,
            DashboardFormatter.formatSteps(aggregateSteps(dayStart, dayEnd)),
            R.string.dashboard_sub_today,
        )
        HealthDataType.DISTANCE -> DashboardMetric(
            type,
            DashboardFormatter.formatDistanceKm(aggregateDistanceMeters(dayStart, dayEnd)),
            R.string.dashboard_sub_km_today,
        )
        HealthDataType.ACTIVE_CALORIES -> DashboardMetric(
            type,
            DashboardFormatter.formatCalories(aggregateActiveCalories(dayStart, dayEnd)),
            R.string.dashboard_sub_kcal_today,
        )
        HealthDataType.TOTAL_CALORIES -> DashboardMetric(
            type,
            DashboardFormatter.formatCalories(aggregateTotalCalories(dayStart, dayEnd)),
            R.string.dashboard_sub_kcal_today,
        )
        HealthDataType.SLEEP -> {
            val sessions = readSleepSessionsInRange(dayStart, dayEnd)
            DashboardMetric(
                type,
                DashboardFormatter.formatDurationMinutes(
                    DashboardFormatter.sumDurationsMinutes(
                        sessions.map { Duration.between(it.startTime, it.endTime) },
                    ),
                ),
                R.string.dashboard_sub_today,
            )
        }
        HealthDataType.HEART_RATE -> {
            val avg = averageHeartRateBpm(dayStart, dayEnd)
            DashboardMetric(
                type,
                DashboardFormatter.formatOptional(avg, DashboardFormatter::formatAverageBpm),
                if (avg != null) R.string.dashboard_sub_avg_bpm else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.HEART_RATE_VARIABILITY -> {
            val avg = averageHeartRateVariabilityMs(dayStart, dayEnd)
            DashboardMetric(
                type,
                DashboardFormatter.formatOptional(avg, DashboardFormatter::formatHeartRateVariabilityMs),
                if (avg != null) R.string.dashboard_sub_ms_avg else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.WEIGHT -> latestMetric(
            type,
            readWeightData(dayStart, dayEnd, null).maxByOrNull { it.time }?.kilograms,
            DashboardFormatter::formatWeightKg,
            R.string.dashboard_sub_kg_latest,
        )
        HealthDataType.HEIGHT -> latestMetric(
            type,
            readHeightData(dayStart, dayEnd, null).maxByOrNull { it.time }?.meters,
            DashboardFormatter::formatHeightCm,
            R.string.dashboard_sub_cm_latest,
        )
        HealthDataType.BLOOD_PRESSURE -> {
            val latest = readBloodPressureData(dayStart, dayEnd, null).maxByOrNull { it.time }
            DashboardMetric(
                type,
                if (latest != null) {
                    DashboardFormatter.formatBloodPressure(latest.systolic, latest.diastolic)
                } else {
                    DashboardFormatter.NO_DATA
                },
                if (latest != null) R.string.dashboard_sub_mmhg_latest else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.BLOOD_GLUCOSE -> latestMetric(
            type,
            readBloodGlucoseData(dayStart, dayEnd, null).maxByOrNull { it.time }?.mmolPerLiter,
            DashboardFormatter::formatBloodGlucose,
            R.string.dashboard_sub_mmol_latest,
        )
        HealthDataType.OXYGEN_SATURATION -> latestMetric(
            type,
            readOxygenSaturationData(dayStart, dayEnd, null).maxByOrNull { it.time }?.percentage,
            DashboardFormatter::formatPercentage,
            R.string.dashboard_sub_percent_latest,
        )
        HealthDataType.BODY_TEMPERATURE -> latestMetric(
            type,
            readBodyTemperatureData(dayStart, dayEnd, null).maxByOrNull { it.time }?.celsius,
            DashboardFormatter::formatCelsius,
            R.string.dashboard_sub_celsius_latest,
        )
        HealthDataType.SKIN_TEMPERATURE -> {
            val latest = readSkinTemperatureData(dayStart, dayEnd, null).maxByOrNull { it.time }
            DashboardMetric(
                type,
                if (latest != null) {
                    DashboardFormatter.formatSignedCelsius(latest.deltaCelsius)
                } else {
                    DashboardFormatter.NO_DATA
                },
                if (latest != null) R.string.dashboard_sub_delta_celsius else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.RESPIRATORY_RATE -> {
            val avg = averageRespiratoryRate(dayStart, dayEnd)
            DashboardMetric(
                type,
                DashboardFormatter.formatOptional(avg, DashboardFormatter::formatRespiratoryRate),
                if (avg != null) R.string.dashboard_sub_per_min_avg else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.RESTING_HEART_RATE -> latestMetric(
            type,
            readRestingHeartRateData(dayStart, dayEnd, null).maxByOrNull { it.time }?.bpm?.toDouble(),
            DashboardFormatter::formatAverageBpm,
            R.string.dashboard_sub_bpm_latest,
        )
        HealthDataType.EXERCISE -> {
            val sessions = readExerciseData(dayStart, dayEnd, null, includeDistance = false, includeSteps = false)
            val totalMinutes = DashboardFormatter.sumDurationsMinutes(sessions.map { it.duration })
            DashboardMetric(
                type,
                if (sessions.isEmpty()) {
                    DashboardFormatter.formatDurationMinutes(0)
                } else {
                    DashboardFormatter.formatDurationMinutes(totalMinutes)
                },
                R.string.dashboard_sub_sessions_today,
            )
        }
        HealthDataType.HYDRATION -> DashboardMetric(
            type,
            DashboardFormatter.formatLiters(
                readHydrationData(dayStart, dayEnd, null).sumOf { it.liters },
            ),
            R.string.dashboard_sub_liters_today,
        )
        HealthDataType.NUTRITION -> DashboardMetric(
            type,
            DashboardFormatter.formatCalories(
                readNutritionData(dayStart, dayEnd, null).mapNotNull { it.calories }.sum(),
            ),
            R.string.dashboard_sub_kcal_today,
        )
        HealthDataType.BASAL_METABOLIC_RATE -> latestMetric(
            type,
            readBasalMetabolicRateData(dayStart, dayEnd, null).maxByOrNull { it.time }?.watts,
            DashboardFormatter::formatWatts,
            R.string.dashboard_sub_watts_latest,
        )
        HealthDataType.BODY_FAT -> latestMetric(
            type,
            readBodyFatData(dayStart, dayEnd, null).maxByOrNull { it.time }?.percentage,
            DashboardFormatter::formatPercentage,
            R.string.dashboard_sub_percent_latest,
        )
        HealthDataType.LEAN_BODY_MASS -> latestMetric(
            type,
            readLeanBodyMassData(dayStart, dayEnd, null).maxByOrNull { it.time }?.kilograms,
            DashboardFormatter::formatWeightKg,
            R.string.dashboard_sub_kg_latest,
        )
        HealthDataType.VO2_MAX -> latestMetric(
            type,
            readVo2MaxData(dayStart, dayEnd, null).maxByOrNull { it.time }?.mlPerKgPerMin,
            DashboardFormatter::formatVo2Max,
            R.string.dashboard_sub_vo2_latest,
        )
        HealthDataType.BONE_MASS -> latestMetric(
            type,
            readBoneMassData(dayStart, dayEnd, null).maxByOrNull { it.time }?.kilograms,
            DashboardFormatter::formatWeightKg,
            R.string.dashboard_sub_kg_latest,
        )
        HealthDataType.MENSTRUATION_FLOW -> {
            val latest = readMenstruationFlowData(dayStart, dayEnd, null).maxByOrNull { it.time }
            DashboardMetric(
                type,
                if (latest != null) latest.flow.toString() else DashboardFormatter.NO_DATA,
                if (latest != null) R.string.dashboard_sub_today else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.MENSTRUATION_PERIOD -> {
            val count = readMenstruationPeriodData(dayStart, dayEnd, null).size
            DashboardMetric(
                type,
                if (count > 0) count.toString() else DashboardFormatter.NO_DATA,
                if (count > 0) R.string.dashboard_sub_today else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.INTERMENSTRUAL_BLEEDING -> {
            val count = readIntermenstrualBleedingData(dayStart, dayEnd, null).size
            DashboardMetric(
                type,
                if (count > 0) count.toString() else DashboardFormatter.NO_DATA,
                if (count > 0) R.string.dashboard_sub_today else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.OVULATION_TEST -> {
            val latest = readOvulationTestData(dayStart, dayEnd, null).maxByOrNull { it.time }
            DashboardMetric(
                type,
                if (latest != null) latest.result.toString() else DashboardFormatter.NO_DATA,
                if (latest != null) R.string.dashboard_sub_today else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.CERVICAL_MUCUS -> {
            val latest = readCervicalMucusData(dayStart, dayEnd, null).maxByOrNull { it.time }
            DashboardMetric(
                type,
                if (latest != null) latest.appearance.toString() else DashboardFormatter.NO_DATA,
                if (latest != null) R.string.dashboard_sub_today else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.SEXUAL_ACTIVITY -> {
            val count = readSexualActivityData(dayStart, dayEnd, null).size
            DashboardMetric(
                type,
                if (count > 0) count.toString() else DashboardFormatter.NO_DATA,
                if (count > 0) R.string.dashboard_sub_today else R.string.dashboard_sub_no_data,
            )
        }
        HealthDataType.BASAL_BODY_TEMPERATURE -> latestMetric(
            type,
            readBasalBodyTemperatureData(dayStart, dayEnd, null).maxByOrNull { it.time }?.celsius,
            DashboardFormatter::formatCelsius,
            R.string.dashboard_sub_celsius_latest,
        )
    }

    private fun latestMetric(
        type: HealthDataType,
        value: Double?,
        formatter: (Double) -> String,
        subtitleResId: Int,
    ): DashboardMetric = DashboardMetric(
        type,
        DashboardFormatter.formatOptional(value, formatter),
        if (value != null) subtitleResId else R.string.dashboard_sub_no_data,
    )

    private suspend fun readSleepSessionsInRange(startTime: Instant, endTime: Instant): List<SleepSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return readAllRecords(request)
    }

    private suspend fun averageHeartRateBpm(startTime: Instant, endTime: Instant): Double? {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val samples = readAllRecords(request).flatMap { it.samples }
        if (samples.isEmpty()) return null
        return samples.map { it.beatsPerMinute.toDouble() }.average()
    }

    private suspend fun averageHeartRateVariabilityMs(startTime: Instant, endTime: Instant): Double? {
        val request = ReadRecordsRequest(
            recordType = HeartRateVariabilityRmssdRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val records = readAllRecords(request)
        if (records.isEmpty()) return null
        return records.map { it.heartRateVariabilityMillis }.average()
    }

    private suspend fun averageRespiratoryRate(startTime: Instant, endTime: Instant): Double? {
        val request = ReadRecordsRequest(
            recordType = RespiratoryRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val records = readAllRecords(request)
        if (records.isEmpty()) return null
        return records.map { it.rate }.average()
    }

    private suspend fun aggregateTotalCalories(startTime: Instant, endTime: Instant): Double {
        return readRawTotalCaloriesData(startTime, endTime, null).sumOf { it.calories }
    }

    private suspend fun readStepsData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int
    ): List<StepsData> {
        return when {
            resolutionMinutes < 0 -> readDailyStepsData(startTime, endTime, lastSync)
            resolutionMinutes == 0 -> readRawStepsData(startTime, endTime, lastSync)
            else -> readBucketedStepsData(startTime, endTime, lastSync, resolutionMinutes)
        }
    }

    private suspend fun readRawStepsData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?
    ): List<StepsData> {
        // Full resolution: forward each Health Connect step interval as-is.
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .filter { it.count > 0 }
            .map { StepsData(count = it.count, startTime = it.startTime, endTime = it.endTime, metadata = it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBucketedStepsData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        bucketMinutes: Int
    ): List<StepsData> {
        // Group raw step intervals into fixed time buckets (keyed by start time)
        // and sum their counts, emitting one StepsData per bucket.
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val bucketSeconds = bucketMinutes.toLong() * 60L
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .filter { it.count > 0 }
            .groupBy { it.startTime.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, records) ->
                val bucketStart = Instant.ofEpochSecond(bucketIndex * bucketSeconds)
                StepsData(
                    count = records.sumOf { it.count },
                    startTime = bucketStart,
                    endTime = bucketStart.plusSeconds(bucketSeconds)
                )
            }
    }

    private suspend fun readDailyStepsData(
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

            val aggregateRequest = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val aggregateResponse = aggregateRL(aggregateRequest)
            val aggregateSteps = aggregateResponse[StepsRecord.COUNT_TOTAL]

            val daySteps: Long = if (aggregateSteps != null && aggregateSteps > 0L) {
                aggregateSteps
            } else {
                // Aggregate returned null, fall back to summing raw records.
                val rawRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
                )
                readAllRecords(rawRequest).sumOf { it.count }
            }

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
        lastSync: Instant?,
        resolutionMinutes: Int,
    ): List<SleepData> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )

        val response = readAllRecords(request)
        val includeStages = resolutionMinutes != SLEEP_SUMMARY

        return response
            .filter { record ->
                lastSync == null || record.endTime >= lastSync
            }
            .map { record ->
                val stages = if (includeStages) {
                    record.stages.map { stage ->
                        SleepStage(
                            stage = stage.stage.toString(),
                            startTime = stage.startTime,
                            endTime = stage.endTime,
                            duration = Duration.between(stage.startTime, stage.endTime)
                        )
                    }
                } else {
                    emptyList()
                }

                SleepData(
                    sessionEndTime = record.endTime,
                    duration = Duration.between(record.startTime, record.endTime),
                    stages = stages,
                    metadata = record.metadata.toRecordMetadata()
                )
            }
    }

    private suspend fun readHeartRateData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        downsampleMinutes: Int
    ): List<HeartRateData> {
        val request = ReadRecordsRequest(recordType = HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        val samples = response.flatMap { record ->
            val metadata = record.metadata.toRecordMetadata()
            record.samples
                .filter { lastSync == null || it.time >= lastSync }
                .map { Triple(it.time, it.beatsPerMinute, metadata) }
        }

        if (downsampleMinutes <= 0) {
            return samples.map { (time, bpm, metadata) ->
                HeartRateData(bpm = bpm, time = time, metadata = metadata)
            }
        }

        val bucketSeconds = downsampleMinutes.toLong() * 60L
        return samples
            .groupBy { it.first.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, bucketSamples) ->
                val bpms = bucketSamples.map { it.second }
                HeartRateData(
                    bpm = bpms.average().roundToLong(),
                    time = Instant.ofEpochSecond(bucketIndex * bucketSeconds),
                    min = bpms.min(),
                    max = bpms.max(),
                    metadata = bucketSamples.first().third,
                )
            }
    }

    private suspend fun readHeartRateVariabilityData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int,
    ): List<HeartRateVariabilityData> {
        val request = ReadRecordsRequest(
            recordType = HeartRateVariabilityRmssdRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val samples = readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { Triple(it.time, it.heartRateVariabilityMillis, it.metadata.toRecordMetadata()) }

        if (resolutionMinutes <= 0) {
            return samples.map { (time, value, metadata) ->
                HeartRateVariabilityData(rmssdMillis = value, time = time, metadata = metadata)
            }
        }

        val bucketSeconds = resolutionMinutes.toLong() * 60L
        return samples
            .groupBy { it.first.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, bucketSamples) ->
                val values = bucketSamples.map { it.second }
                HeartRateVariabilityData(
                    rmssdMillis = values.average(),
                    time = Instant.ofEpochSecond(bucketIndex * bucketSeconds),
                    min = values.min(),
                    max = values.max(),
                    metadata = bucketSamples.first().third,
                )
            }
    }

    private suspend fun readDistanceData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int,
    ): List<DistanceData> {
        return when {
            resolutionMinutes < 0 -> readDailyDistanceData(startTime, endTime, lastSync)
            resolutionMinutes == 0 -> readRawDistanceData(startTime, endTime, lastSync)
            else -> readBucketedDistanceData(startTime, endTime, lastSync, resolutionMinutes)
        }
    }

    private suspend fun readRawDistanceData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<DistanceData> {
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .filter { it.distance.inMeters > 0.0 }
            .map { DistanceData(it.distance.inMeters, it.startTime, it.endTime, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBucketedDistanceData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        bucketMinutes: Int,
    ): List<DistanceData> {
        val bucketSeconds = bucketMinutes.toLong() * 60L
        return readRawDistanceData(startTime, endTime, lastSync)
            .groupBy { it.startTime.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, records) ->
                val bucketStart = Instant.ofEpochSecond(bucketIndex * bucketSeconds)
                DistanceData(
                    meters = records.sumOf { it.meters },
                    startTime = bucketStart,
                    endTime = bucketStart.plusSeconds(bucketSeconds),
                )
            }
    }

    private suspend fun readDailyDistanceData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<DistanceData> {
        // Aggregate distance per calendar day (same pattern as steps).
        // If the aggregate returns null (e.g., Google Health/Fit data after the Fitbit rebrand),
        // fall back to summing raw DistanceRecord entries so distance is never silently omitted.
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

            val aggregateRequest = AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val aggregateResponse = aggregateRL(aggregateRequest)
            val aggregateMeters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters

            val dayDistance: Double = if (aggregateMeters != null && aggregateMeters > 0.0) {
                aggregateMeters
            } else {
                // Aggregate returned null or zero — fall back to raw records.
                val rawRequest = ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
                )
                readAllRecords(rawRequest).sumOf { it.distance.inMeters }
            }

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

    private suspend fun readActiveCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int,
    ): List<ActiveCaloriesData> {
        return when {
            resolutionMinutes < 0 -> readDailyActiveCaloriesData(startTime, endTime, lastSync)
            resolutionMinutes == 0 -> readRawActiveCaloriesData(startTime, endTime, lastSync)
            else -> readBucketedActiveCaloriesData(startTime, endTime, lastSync, resolutionMinutes)
        }
    }

    private suspend fun readRawActiveCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<ActiveCaloriesData> {
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .filter { it.energy.inKilocalories > 0.0 }
            .map { ActiveCaloriesData(it.energy.inKilocalories, it.startTime, it.endTime, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBucketedActiveCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        bucketMinutes: Int,
    ): List<ActiveCaloriesData> {
        val bucketSeconds = bucketMinutes.toLong() * 60L
        return readRawActiveCaloriesData(startTime, endTime, lastSync)
            .groupBy { it.startTime.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, records) ->
                val bucketStart = Instant.ofEpochSecond(bucketIndex * bucketSeconds)
                ActiveCaloriesData(
                    calories = records.sumOf { it.calories },
                    startTime = bucketStart,
                    endTime = bucketStart.plusSeconds(bucketSeconds),
                )
            }
    }

    private suspend fun readDailyActiveCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<ActiveCaloriesData> {
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

            val aggregateRequest = AggregateRequest(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
            )
            val aggregateResponse = aggregateRL(aggregateRequest)
            val aggregateKcal = aggregateResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories

            val dayCalories: Double = if (aggregateKcal != null && aggregateKcal > 0.0) {
                aggregateKcal
            } else {
                // Aggregate returned null — fall back to summing raw records.
                val rawRequest = ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(queryStart, queryEnd)
                )
                readAllRecords(rawRequest).sumOf { it.energy.inKilocalories }
            }

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

    private suspend fun readTotalCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int,
    ): List<TotalCaloriesData> {
        return when {
            resolutionMinutes < 0 -> readDailyTotalCaloriesData(startTime, endTime, lastSync)
            resolutionMinutes == 0 -> readRawTotalCaloriesData(startTime, endTime, lastSync)
            else -> readBucketedTotalCaloriesData(startTime, endTime, lastSync, resolutionMinutes)
        }
    }

    private suspend fun readRawTotalCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<TotalCaloriesData> {
        val request = ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map { TotalCaloriesData(it.energy.inKilocalories, it.startTime, it.endTime, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBucketedTotalCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        bucketMinutes: Int,
    ): List<TotalCaloriesData> {
        val bucketSeconds = bucketMinutes.toLong() * 60L
        return readRawTotalCaloriesData(startTime, endTime, lastSync)
            .groupBy { it.startTime.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, records) ->
                val bucketStart = Instant.ofEpochSecond(bucketIndex * bucketSeconds)
                TotalCaloriesData(
                    calories = records.sumOf { it.calories },
                    startTime = bucketStart,
                    endTime = bucketStart.plusSeconds(bucketSeconds),
                )
            }
    }

    private suspend fun readDailyTotalCaloriesData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<TotalCaloriesData> {
        val zone = java.time.ZoneId.systemDefault()
        val result = mutableListOf<TotalCaloriesData>()
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

            val dayCalories = readRawTotalCaloriesData(queryStart, queryEnd, null)
                .sumOf { it.calories }

            if (dayCalories > 0.0) {
                result.add(
                    TotalCaloriesData(
                        calories = dayCalories,
                        startTime = dayStart,
                        endTime = queryEnd,
                    ),
                )
            }
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    private suspend fun readWeightData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<WeightData> {
        val request = ReadRecordsRequest(recordType = WeightRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { WeightData(it.weight.inKilograms, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readHeightData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<HeightData> {
        val request = ReadRecordsRequest(recordType = HeightRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { HeightData(it.height.inMeters, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBloodPressureData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BloodPressureData> {
        val request = ReadRecordsRequest(recordType = BloodPressureRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { BloodPressureData(it.systolic.inMillimetersOfMercury, it.diastolic.inMillimetersOfMercury, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBloodGlucoseData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BloodGlucoseData> {
        val request = ReadRecordsRequest(recordType = BloodGlucoseRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { BloodGlucoseData(it.level.inMillimolesPerLiter, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readOxygenSaturationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int = RESOLUTION_FULL,
    ): List<OxygenSaturationData> {
        val request = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val samples = readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { Triple(it.time, it.percentage.value, it.metadata.toRecordMetadata()) }

        if (resolutionMinutes <= 0) {
            return samples.map { (time, value, metadata) ->
                OxygenSaturationData(percentage = value, time = time, metadata = metadata)
            }
        }

        val bucketSeconds = resolutionMinutes.toLong() * 60L
        return samples
            .groupBy { it.first.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, bucketSamples) ->
                val values = bucketSamples.map { it.second }
                OxygenSaturationData(
                    percentage = values.average(),
                    time = Instant.ofEpochSecond(bucketIndex * bucketSeconds),
                    min = values.min(),
                    max = values.max(),
                    metadata = bucketSamples.first().third,
                )
            }
    }

    private suspend fun readBodyTemperatureData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BodyTemperatureData> {
        val request = ReadRecordsRequest(recordType = BodyTemperatureRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { BodyTemperatureData(it.temperature.inCelsius, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readSkinTemperatureData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int = RESOLUTION_FULL,
    ): List<SkinTemperatureData> {
        val request = ReadRecordsRequest(
            recordType = SkinTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val samples = readAllRecords(request).flatMap { record ->
            record.deltas
                .filter { lastSync == null || it.time >= lastSync }
                .map { delta ->
                    SkinTemperatureData(
                        time = delta.time,
                        deltaCelsius = delta.delta.inCelsius,
                        baselineCelsius = record.baseline?.inCelsius,
                        measurementLocation = record.measurementLocation,
                        metadata = record.metadata.toRecordMetadata(),
                    )
                }
        }

        if (resolutionMinutes <= 0) {
            return samples
        }

        val bucketSeconds = resolutionMinutes.toLong() * 60L
        return samples
            .groupBy { it.time.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, bucketSamples) ->
                val deltas = bucketSamples.map { it.deltaCelsius }
                val first = bucketSamples.first()
                SkinTemperatureData(
                    time = Instant.ofEpochSecond(bucketIndex * bucketSeconds),
                    deltaCelsius = deltas.average(),
                    baselineCelsius = first.baselineCelsius,
                    measurementLocation = first.measurementLocation,
                    minDeltaCelsius = deltas.min(),
                    maxDeltaCelsius = deltas.max(),
                    metadata = first.metadata,
                )
            }
    }

    private suspend fun readRespiratoryRateData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int = RESOLUTION_FULL,
    ): List<RespiratoryRateData> {
        val request = ReadRecordsRequest(
            recordType = RespiratoryRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        val samples = readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { Triple(it.time, it.rate, it.metadata.toRecordMetadata()) }

        if (resolutionMinutes <= 0) {
            return samples.map { (time, value, metadata) ->
                RespiratoryRateData(rate = value, time = time, metadata = metadata)
            }
        }

        val bucketSeconds = resolutionMinutes.toLong() * 60L
        return samples
            .groupBy { it.first.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, bucketSamples) ->
                val values = bucketSamples.map { it.second }
                RespiratoryRateData(
                    rate = values.average(),
                    time = Instant.ofEpochSecond(bucketIndex * bucketSeconds),
                    min = values.min(),
                    max = values.max(),
                    metadata = bucketSamples.first().third,
                )
            }
    }

    private suspend fun readRestingHeartRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<RestingHeartRateData> {
        val request = ReadRecordsRequest(recordType = RestingHeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { RestingHeartRateData(it.beatsPerMinute, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readExerciseData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        includeDistance: Boolean,
        includeSteps: Boolean
    ): List<ExerciseData> {
        val request = ReadRecordsRequest(recordType = ExerciseSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.endTime >= lastSync }
            .map {
                val duration = Duration.between(it.startTime, it.endTime)
                val distanceMeters = if (includeDistance) readDistanceTotal(it.startTime, it.endTime) else null
                val steps = if (includeSteps) readStepsTotal(it.startTime, it.endTime) else null
                val cadenceMetrics = if (includeSteps) readStepsCadenceMetrics(it.startTime, it.endTime) else StepsCadenceMetrics()
                ExerciseData(
                    type = it.exerciseType.toString(),
                    startTime = it.startTime,
                    endTime = it.endTime,
                    duration = duration,
                    distanceMeters = distanceMeters,
                    steps = steps,
                    avgCadenceSpm = cadenceMetrics.avg ?: deriveAverageCadenceSpm(steps, duration),
                    maxCadenceSpm = cadenceMetrics.max,
                    strideLengthMeters = deriveStrideLengthMeters(distanceMeters, steps),
                    metadata = it.metadata.toRecordMetadata()
                )
            }
    }

    private data class StepsCadenceMetrics(
        val avg: Double? = null,
        val max: Double? = null
    )

    private suspend fun aggregateSteps(startTime: Instant, endTime: Instant): Long {
        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return aggregateRL(request)[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    private suspend fun aggregateDistanceMeters(startTime: Instant, endTime: Instant): Double {
        val request = AggregateRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return aggregateRL(request)[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
    }

    private suspend fun aggregateActiveCalories(startTime: Instant, endTime: Instant): Double {
        val request = AggregateRequest(
            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return aggregateRL(request)[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            ?.inKilocalories ?: 0.0
    }

    private suspend fun readStepsTotal(startTime: Instant, endTime: Instant): Long? {
        val steps = aggregateSteps(startTime, endTime)
        return steps.takeIf { it > 0L }
    }

    private suspend fun readStepsCadenceMetrics(startTime: Instant, endTime: Instant): StepsCadenceMetrics {
        return try {
            val request = AggregateRequest(
                metrics = setOf(StepsCadenceRecord.RATE_AVG, StepsCadenceRecord.RATE_MAX),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            val response = aggregateRL(request)
            StepsCadenceMetrics(
                avg = response[StepsCadenceRecord.RATE_AVG]?.takeIf { it > 0.0 },
                max = response[StepsCadenceRecord.RATE_MAX]?.takeIf { it > 0.0 }
            )
        } catch (_: Exception) {
            // Cadence is optional. Keep exercise sync working on providers that expose steps but not cadence.
            StepsCadenceMetrics()
        }
    }

    private fun deriveAverageCadenceSpm(steps: Long?, duration: Duration): Double? {
        if (steps == null || steps <= 0L || duration.isZero || duration.isNegative) {
            return null
        }
        val durationMinutes = duration.toMillis() / 60000.0
        return (steps.toDouble() / durationMinutes).takeIf { it > 0.0 }
    }

    private fun deriveStrideLengthMeters(distanceMeters: Double?, steps: Long?): Double? {
        if (distanceMeters == null || distanceMeters <= 0.0 || steps == null || steps <= 0L) {
            return null
        }
        return distanceMeters / steps.toDouble()
    }

    private suspend fun readDistanceTotal(startTime: Instant, endTime: Instant): Double? {
        val distanceMeters = aggregateDistanceMeters(startTime, endTime)
        return distanceMeters.takeIf { it > 0.0 }
    }

    private suspend fun readHydrationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int = RESOLUTION_FULL,
    ): List<HydrationData> {
        return when {
            resolutionMinutes < 0 -> readDailyHydrationData(startTime, endTime, lastSync)
            resolutionMinutes == 0 -> readRawHydrationData(startTime, endTime, lastSync)
            else -> readBucketedHydrationData(startTime, endTime, lastSync, resolutionMinutes)
        }
    }

    private suspend fun readRawHydrationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<HydrationData> {
        val request = ReadRecordsRequest(
            recordType = HydrationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map { HydrationData(it.volume.inLiters, it.startTime, it.endTime, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBucketedHydrationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        bucketMinutes: Int,
    ): List<HydrationData> {
        val bucketSeconds = bucketMinutes.toLong() * 60L
        return readRawHydrationData(startTime, endTime, lastSync)
            .groupBy { it.startTime.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, records) ->
                val bucketStart = Instant.ofEpochSecond(bucketIndex * bucketSeconds)
                HydrationData(
                    liters = records.sumOf { it.liters },
                    startTime = bucketStart,
                    endTime = bucketStart.plusSeconds(bucketSeconds),
                )
            }
    }

    private suspend fun readDailyHydrationData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<HydrationData> {
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<HydrationData>()
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

            val dayLiters = readRawHydrationData(queryStart, queryEnd, null).sumOf { it.liters }
            if (dayLiters > 0.0) {
                result.add(HydrationData(dayLiters, dayStart, queryEnd))
            }
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    private suspend fun readNutritionData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        resolutionMinutes: Int = RESOLUTION_FULL,
    ): List<NutritionData> {
        return when {
            resolutionMinutes < 0 -> readDailyNutritionData(startTime, endTime, lastSync)
            resolutionMinutes == 0 -> readRawNutritionData(startTime, endTime, lastSync)
            else -> readBucketedNutritionData(startTime, endTime, lastSync, resolutionMinutes)
        }
    }

    private suspend fun readRawNutritionData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<NutritionData> {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        )
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map {
                NutritionData(
                    calories = it.energy?.inKilocalories,
                    protein = it.protein?.inGrams,
                    carbs = it.totalCarbohydrate?.inGrams,
                    fat = it.totalFat?.inGrams,
                    sugar = it.sugar?.inGrams,
                    sodium = it.sodium?.inGrams,
                    dietaryFiber = it.dietaryFiber?.inGrams,
                    name = it.name,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    metadata = it.metadata.toRecordMetadata(),
                )
            }
    }

    private fun mergeNutritionRecords(records: List<NutritionData>, startTime: Instant, endTime: Instant): NutritionData {
        return NutritionData(
            calories = records.mapNotNull { it.calories }.takeIf { it.isNotEmpty() }?.sum(),
            protein = records.mapNotNull { it.protein }.takeIf { it.isNotEmpty() }?.sum(),
            carbs = records.mapNotNull { it.carbs }.takeIf { it.isNotEmpty() }?.sum(),
            fat = records.mapNotNull { it.fat }.takeIf { it.isNotEmpty() }?.sum(),
            sugar = records.mapNotNull { it.sugar }.takeIf { it.isNotEmpty() }?.sum(),
            sodium = records.mapNotNull { it.sodium }.takeIf { it.isNotEmpty() }?.sum(),
            dietaryFiber = records.mapNotNull { it.dietaryFiber }.takeIf { it.isNotEmpty() }?.sum(),
            name = null,
            startTime = startTime,
            endTime = endTime,
        )
    }

    private suspend fun readBucketedNutritionData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
        bucketMinutes: Int,
    ): List<NutritionData> {
        val bucketSeconds = bucketMinutes.toLong() * 60L
        return readRawNutritionData(startTime, endTime, lastSync)
            .groupBy { it.startTime.epochSecond / bucketSeconds }
            .toSortedMap()
            .map { (bucketIndex, records) ->
                val bucketStart = Instant.ofEpochSecond(bucketIndex * bucketSeconds)
                val bucketEnd = bucketStart.plusSeconds(bucketSeconds)
                mergeNutritionRecords(records, bucketStart, bucketEnd)
            }
    }

    private suspend fun readDailyNutritionData(
        startTime: Instant,
        endTime: Instant,
        lastSync: Instant?,
    ): List<NutritionData> {
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<NutritionData>()
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

            val dayRecords = readRawNutritionData(queryStart, queryEnd, null)
            if (dayRecords.isNotEmpty()) {
                result.add(mergeNutritionRecords(dayRecords, dayStart, queryEnd))
            }
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    private suspend fun readBasalMetabolicRateData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BasalMetabolicRateData> {
        val request = ReadRecordsRequest(recordType = BasalMetabolicRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { BasalMetabolicRateData(it.basalMetabolicRate.inWatts, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBodyFatData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BodyFatData> {
        val request = ReadRecordsRequest(recordType = BodyFatRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { BodyFatData(it.percentage.value, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readLeanBodyMassData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<LeanBodyMassData> {
        val request = ReadRecordsRequest(recordType = LeanBodyMassRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { LeanBodyMassData(it.mass.inKilograms, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readVo2MaxData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<Vo2MaxData> {
        val request = ReadRecordsRequest(recordType = Vo2MaxRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { Vo2MaxData(it.vo2MillilitersPerMinuteKilogram, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBoneMassData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BoneMassData> {
        val request = ReadRecordsRequest(recordType = BoneMassRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        val response = readAllRecords(request)
        return response.filter { lastSync == null || it.time >= lastSync }
            .map { BoneMassData(it.mass.inKilograms, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readMenstruationFlowData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<MenstruationFlowData> {
        val request = ReadRecordsRequest(recordType = MenstruationFlowRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { MenstruationFlowData(it.flow, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readMenstruationPeriodData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<MenstruationPeriodData> {
        val request = ReadRecordsRequest(recordType = MenstruationPeriodRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.endTime >= lastSync }
            .map { MenstruationPeriodData(it.startTime, it.endTime, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readIntermenstrualBleedingData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<IntermenstrualBleedingData> {
        val request = ReadRecordsRequest(recordType = IntermenstrualBleedingRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { IntermenstrualBleedingData(it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readOvulationTestData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<OvulationTestData> {
        val request = ReadRecordsRequest(recordType = OvulationTestRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { OvulationTestData(it.result, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readCervicalMucusData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<CervicalMucusData> {
        val request = ReadRecordsRequest(recordType = CervicalMucusRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { CervicalMucusData(it.appearance, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readSexualActivityData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<SexualActivityData> {
        val request = ReadRecordsRequest(recordType = SexualActivityRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { SexualActivityData(it.protectionUsed, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun readBasalBodyTemperatureData(startTime: Instant, endTime: Instant, lastSync: Instant?): List<BasalBodyTemperatureData> {
        val request = ReadRecordsRequest(recordType = BasalBodyTemperatureRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime))
        return readAllRecords(request)
            .filter { lastSync == null || it.time >= lastSync }
            .map { BasalBodyTemperatureData(it.temperature.inCelsius, it.measurementLocation, it.time, it.metadata.toRecordMetadata()) }
    }

    private suspend fun <T : Record> readAllRecords(request: ReadRecordsRequest<T>): List<T> {
        val all = mutableListOf<T>()
        var token: String? = null
        var firstPage = true
        val throttlePages = request.recordType in PAGINATED_READ_THROTTLE_TYPES
        do {
            // Gentle inter-page throttle on heavy, multi-page reads (e.g. heart rate over a
            // long window) so they don't trip Health Connect's burst quota in the first place.
            if (throttlePages && !firstPage) delay(READ_PAGE_DELAY_MS)
            firstPage = false
            val current = if (token == null) request else ReadRecordsRequest(
                recordType = request.recordType,
                timeRangeFilter = request.timeRangeFilter,
                dataOriginFilter = request.dataOriginFilter,
                ascendingOrder = request.ascendingOrder,
                pageSize = request.pageSize,
                pageToken = token,
            )
            val page = withHealthConnectRetry { healthConnectClient.readRecords(current) }
            all.addAll(page.records)
            token = page.pageToken
        } while (token != null)
        return all
    }

    /**
     * Retries a Health Connect call when the platform throws its rate-limit /
     * quota-exceeded error, backing off exponentially (1s, 2s, 4s, 8s). Any other
     * error propagates immediately. This mitigates the periodic (burst) quota; the
     * separate daily quota still needs real time to replenish, so after the last
     * attempt the original exception is rethrown for the UI to surface.
     */
    private suspend fun <T> withHealthConnectRetry(
        maxAttempts: Int = RATE_LIMIT_MAX_ATTEMPTS,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        var delayMs = RATE_LIMIT_INITIAL_DELAY_MS
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val msg = e.message?.lowercase().orEmpty()
                val isRateLimit = msg.contains("rate limit") || msg.contains("quota")
                attempt++
                if (!isRateLimit || attempt >= maxAttempts) throw e
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(RATE_LIMIT_MAX_DELAY_MS)
            }
        }
    }

    private suspend fun aggregateRL(request: AggregateRequest) =
        withHealthConnectRetry { healthConnectClient.aggregate(request) }

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

        private const val RATE_LIMIT_MAX_ATTEMPTS = 4
        private const val RATE_LIMIT_INITIAL_DELAY_MS = 1_000L
        private const val RATE_LIMIT_MAX_DELAY_MS = 8_000L
        private const val READ_PAGE_DELAY_MS = 120L

        /** Record types that commonly paginate and benefit from inter-page throttling. */
        private val PAGINATED_READ_THROTTLE_TYPES = setOf(
            HeartRateRecord::class,
            HeartRateVariabilityRmssdRecord::class,
            StepsRecord::class,
            SleepSessionRecord::class,
            ExerciseSessionRecord::class,
        )

        fun grantedDataTypes(grantedPermissions: Set<String>): List<HealthDataType> =
            HealthDataType.entries.filter { type ->
                HealthPermission.getReadPermission(type.recordClass) in grantedPermissions
            }

        fun getPermissionsForTypes(
            types: Set<HealthDataType>,
            includeBackgroundPermission: Boolean = true,
            includeHistoryPermission: Boolean = true,
            includeStepsCadence: Boolean = true
        ): Set<String> {
            val permissions = types.map { HealthPermission.getReadPermission(it.recordClass) }.toMutableSet()
            if (includeStepsCadence && HealthDataType.STEPS in types) {
                permissions.add(HealthPermission.getReadPermission(StepsCadenceRecord::class))
            }
            if (includeBackgroundPermission) {
                permissions.add(BACKGROUND_PERMISSION_STR)
            }
            if (includeHistoryPermission) {
                permissions.add("android.permission.health.READ_HEALTH_DATA_HISTORY")
            }
            return permissions
        }

        const val BACKGROUND_PERMISSION_STR = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        val INITIAL_PERMISSIONS: Set<String>
            get() = ALL_PERMISSIONS + BACKGROUND_PERMISSION_STR

        val ALL_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(StepsCadenceRecord::class),
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
            HealthPermission.getReadPermission(SkinTemperatureRecord::class),
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
            HealthPermission.getReadPermission(MenstruationFlowRecord::class),
            HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
            HealthPermission.getReadPermission(IntermenstrualBleedingRecord::class),
            HealthPermission.getReadPermission(OvulationTestRecord::class),
            HealthPermission.getReadPermission(CervicalMucusRecord::class),
            HealthPermission.getReadPermission(SexualActivityRecord::class),
            HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
            "android.permission.health.READ_HEALTH_DATA_HISTORY"
        )
    }
}
