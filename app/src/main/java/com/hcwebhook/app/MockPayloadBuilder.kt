package com.hcwebhook.app

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Generates realistic mock health data for the webhook **Test** button.
 *
 * - [build] → JSON string (HTTP webhooks)
 * - [buildHealthData] → [HealthData] (gRPC / protobuf webhooks via [ProtobufPayloadBuilder])
 *
 * Keep both paths aligned. Pass [enabledTypes] to match the webhook filter;
 * null includes all types.
 */
object MockPayloadBuilder {

    private val fitnessMeta = RecordMetadata(
        dataOrigin = "com.google.android.apps.fitness",
        recordingMethod = "automatically_recorded",
        deviceManufacturer = "Google",
        deviceModel = "Pixel Watch",
        deviceType = 1
    )

    fun build(enabledTypes: Set<String>? = null, appVersion: String = "unknown"): String {
        val data = buildHealthData(enabledTypes)
        val now = Instant.now()
        // Prefer the same logical records as gRPC; emit JSON by hand so we can
        // keep the explicit "test": true flag (not part of HealthData / proto).
        return buildJsonObject {
            put("timestamp", now.toString())
            put("app_version", appVersion)
            put("test", true)

            if (data.steps.isNotEmpty()) {
                putJsonArray("steps") {
                    data.steps.forEach { step ->
                        add(buildJsonObject {
                            put("count", step.count)
                            put("start_time", step.startTime.toString())
                            put("end_time", step.endTime.toString())
                            step.metadata?.let { putMetadata(it) }
                        })
                    }
                }
            }
            if (data.sleep.isNotEmpty()) {
                putJsonArray("sleep") {
                    data.sleep.forEach { sleep ->
                        add(buildJsonObject {
                            put("session_end_time", sleep.sessionEndTime.toString())
                            put("duration_seconds", sleep.duration.seconds)
                            sleep.sessionUpdatedAt?.let { put("sleep_updated_at", it.toString()) }
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
                            sleep.metadata?.let { putMetadata(it) }
                        })
                    }
                }
            }
            if (data.heartRate.isNotEmpty()) {
                putJsonArray("heart_rate") {
                    data.heartRate.forEach { hr ->
                        add(buildJsonObject {
                            put("bpm", hr.bpm)
                            put("time", hr.time.toString())
                            hr.metadata?.let { putMetadata(it) }
                        })
                    }
                }
            }
            if (data.heartRateVariability.isNotEmpty()) {
                putJsonArray("heart_rate_variability") {
                    data.heartRateVariability.forEach { hrv ->
                        add(buildJsonObject {
                            put("rmssd_millis", hrv.rmssdMillis)
                            put("time", hrv.time.toString())
                            hrv.metadata?.let { putMetadata(it) }
                        })
                    }
                }
            }
            if (data.distance.isNotEmpty()) {
                putJsonArray("distance") {
                    data.distance.forEach {
                        add(buildJsonObject {
                            put("meters", it.meters)
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.activeCalories.isNotEmpty()) {
                putJsonArray("active_calories") {
                    data.activeCalories.forEach {
                        add(buildJsonObject {
                            put("calories", it.calories)
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.totalCalories.isNotEmpty()) {
                putJsonArray("total_calories") {
                    data.totalCalories.forEach {
                        add(buildJsonObject {
                            put("calories", it.calories)
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.weight.isNotEmpty()) {
                putJsonArray("weight") {
                    data.weight.forEach {
                        add(buildJsonObject {
                            put("kilograms", it.kilograms)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.height.isNotEmpty()) {
                putJsonArray("height") {
                    data.height.forEach {
                        add(buildJsonObject {
                            put("meters", it.meters)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.bloodPressure.isNotEmpty()) {
                putJsonArray("blood_pressure") {
                    data.bloodPressure.forEach {
                        add(buildJsonObject {
                            put("systolic", it.systolic)
                            put("diastolic", it.diastolic)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.bloodGlucose.isNotEmpty()) {
                putJsonArray("blood_glucose") {
                    data.bloodGlucose.forEach {
                        add(buildJsonObject {
                            put("mmol_per_liter", it.mmolPerLiter)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.oxygenSaturation.isNotEmpty()) {
                putJsonArray("oxygen_saturation") {
                    data.oxygenSaturation.forEach {
                        add(buildJsonObject {
                            put("percentage", it.percentage)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.bodyTemperature.isNotEmpty()) {
                putJsonArray("body_temperature") {
                    data.bodyTemperature.forEach {
                        add(buildJsonObject {
                            put("celsius", it.celsius)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.skinTemperature.isNotEmpty()) {
                putJsonArray("skin_temperature") {
                    data.skinTemperature.forEach { skin ->
                        add(buildJsonObject {
                            put("time", skin.time.toString())
                            put("delta_celsius", skin.deltaCelsius)
                            skin.baselineCelsius?.let { put("baseline_celsius", it) }
                            put("measurement_location", skin.measurementLocation)
                            skin.metadata?.let { putMetadata(it) }
                        })
                    }
                }
            }
            if (data.respiratoryRate.isNotEmpty()) {
                putJsonArray("respiratory_rate") {
                    data.respiratoryRate.forEach {
                        add(buildJsonObject {
                            put("rate", it.rate)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.restingHeartRate.isNotEmpty()) {
                putJsonArray("resting_heart_rate") {
                    data.restingHeartRate.forEach {
                        add(buildJsonObject {
                            put("bpm", it.bpm)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.exercise.isNotEmpty()) {
                putJsonArray("exercise") {
                    data.exercise.forEach {
                        add(buildJsonObject {
                            put("type", it.type)
                            it.title?.let { title -> put("title", title) }
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            put("duration_seconds", it.duration.seconds)
                            it.distanceMeters?.let { d -> put("distance_meters", d) }
                            it.steps?.let { s -> put("steps", s) }
                            it.avgCadenceSpm?.let { c -> put("avg_cadence_spm", c) }
                            it.maxCadenceSpm?.let { c -> put("max_cadence_spm", c) }
                            it.strideLengthMeters?.let { s -> put("stride_length_m", s) }
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.hydration.isNotEmpty()) {
                putJsonArray("hydration") {
                    data.hydration.forEach {
                        add(buildJsonObject {
                            put("liters", it.liters)
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.nutrition.isNotEmpty()) {
                putJsonArray("nutrition") {
                    data.nutrition.forEach {
                        add(buildJsonObject {
                            it.calories?.let { c -> put("calories", c) }
                            it.protein?.let { p -> put("protein_grams", p) }
                            it.carbs?.let { c -> put("carbs_grams", c) }
                            it.fat?.let { f -> put("fat_grams", f) }
                            it.sugar?.let { s -> put("sugar_grams", s) }
                            it.sodium?.let { s -> put("sodium_grams", s) }
                            it.dietaryFiber?.let { f -> put("dietary_fiber_grams", f) }
                            it.name?.let { n -> put("name", n) }
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.basalMetabolicRate.isNotEmpty()) {
                putJsonArray("basal_metabolic_rate") {
                    data.basalMetabolicRate.forEach {
                        add(buildJsonObject {
                            put("watts", it.watts)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.bodyFat.isNotEmpty()) {
                putJsonArray("body_fat") {
                    data.bodyFat.forEach {
                        add(buildJsonObject {
                            put("percentage", it.percentage)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.leanBodyMass.isNotEmpty()) {
                putJsonArray("lean_body_mass") {
                    data.leanBodyMass.forEach {
                        add(buildJsonObject {
                            put("kilograms", it.kilograms)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.bodyWaterMass.isNotEmpty()) {
                putJsonArray("body_water_mass") {
                    data.bodyWaterMass.forEach {
                        add(buildJsonObject {
                            put("kilograms", it.kilograms)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.weight.isNotEmpty() && data.height.isNotEmpty()) {
                val w = data.weight.first()
                val h = data.height.first()
                putJsonArray("bmi") {
                    add(buildJsonObject {
                        put("value", w.kilograms / (h.meters * h.meters))
                        put("time", w.time.toString())
                        put("weight_kg", w.kilograms)
                        put("height_meters", h.meters)
                    })
                }
            }
            if (data.vo2Max.isNotEmpty()) {
                putJsonArray("vo2_max") {
                    data.vo2Max.forEach {
                        add(buildJsonObject {
                            put("ml_per_kg_per_min", it.mlPerKgPerMin)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.boneMass.isNotEmpty()) {
                putJsonArray("bone_mass") {
                    data.boneMass.forEach {
                        add(buildJsonObject {
                            put("kilograms", it.kilograms)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.menstruationFlow.isNotEmpty()) {
                putJsonArray("menstruation_flow") {
                    data.menstruationFlow.forEach {
                        add(buildJsonObject {
                            put("flow", it.flow)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.menstruationPeriod.isNotEmpty()) {
                putJsonArray("menstruation_period") {
                    data.menstruationPeriod.forEach {
                        add(buildJsonObject {
                            put("start_time", it.startTime.toString())
                            put("end_time", it.endTime.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.intermenstrualBleeding.isNotEmpty()) {
                putJsonArray("intermenstrual_bleeding") {
                    data.intermenstrualBleeding.forEach {
                        add(buildJsonObject {
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.ovulationTest.isNotEmpty()) {
                putJsonArray("ovulation_test") {
                    data.ovulationTest.forEach {
                        add(buildJsonObject {
                            put("result", it.result)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.cervicalMucus.isNotEmpty()) {
                putJsonArray("cervical_mucus") {
                    data.cervicalMucus.forEach {
                        add(buildJsonObject {
                            put("appearance", it.appearance)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.sexualActivity.isNotEmpty()) {
                putJsonArray("sexual_activity") {
                    data.sexualActivity.forEach {
                        add(buildJsonObject {
                            put("protection_used", it.protectionUsed)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
            if (data.basalBodyTemperature.isNotEmpty()) {
                putJsonArray("basal_body_temperature") {
                    data.basalBodyTemperature.forEach {
                        add(buildJsonObject {
                            put("celsius", it.celsius)
                            put("measurement_location", it.measurementLocation)
                            put("time", it.time.toString())
                            it.metadata?.let { meta -> putMetadata(meta) }
                        })
                    }
                }
            }
        }.toString()
    }

    /**
     * Rich mock [HealthData] used by gRPC Test and as the source for [build].
     */
    fun buildHealthData(enabledTypes: Set<String>? = null): HealthData {
        val now = Instant.now()
        val dayStart = now.truncatedTo(ChronoUnit.DAYS)
        val yesterday = dayStart.minus(1, ChronoUnit.DAYS)
        val t = { h: Long, m: Long -> yesterday.plus(h * 60 + m, ChronoUnit.MINUTES) }
        fun include(type: String) = enabledTypes == null || type in enabledTypes

        val heartSamples = listOf(
            62L to t(6, 5), 58L to t(6, 20), 64L to t(6, 45),
            72L to t(7, 10), 78L to t(7, 40), 85L to t(8, 5),
            110L to t(8, 20), 142L to t(8, 35), 155L to t(8, 50),
            148L to t(9, 0), 120L to t(9, 15), 95L to t(9, 30),
            82L to t(10, 0), 76L to t(11, 0), 74L to t(12, 0),
            88L to t(14, 0), 92L to t(16, 0), 80L to t(18, 0),
            70L to t(20, 0), 65L to t(22, 0)
        )

        return HealthData(
            steps = if (include(HealthDataType.STEPS.name)) listOf(
                StepsData(1240, yesterday, t(6, 0), fitnessMeta),
                StepsData(3180, t(6, 0), t(9, 0), fitnessMeta),
                StepsData(2450, t(9, 0), t(12, 0), fitnessMeta),
                StepsData(1562, t(12, 0), t(18, 0), fitnessMeta),
                StepsData(8432, yesterday, t(10, 30), fitnessMeta)
            ) else emptyList(),
            sleep = if (include(HealthDataType.SLEEP.name)) listOf(
                SleepData(
                    sessionEndTime = t(7, 0),
                    duration = Duration.ofSeconds(27000),
                    stages = listOf(
                        SleepStage("deep", yesterday, t(2, 0), Duration.ofSeconds(7200)),
                        SleepStage("rem", t(2, 0), t(5, 30), Duration.ofSeconds(12600)),
                        SleepStage("light", t(5, 30), t(7, 0), Duration.ofSeconds(5400))
                    ),
                    metadata = fitnessMeta,
                    sessionUpdatedAt = t(7, 0)
                )
            ) else emptyList(),
            heartRate = if (include(HealthDataType.HEART_RATE.name)) {
                heartSamples.map { (bpm, time) -> HeartRateData(bpm, time, metadata = fitnessMeta) }
            } else emptyList(),
            heartRateVariability = if (include(HealthDataType.HEART_RATE_VARIABILITY.name)) listOf(
                HeartRateVariabilityData(38.2, t(6, 10), metadata = fitnessMeta),
                HeartRateVariabilityData(42.5, t(6, 30), metadata = fitnessMeta),
                HeartRateVariabilityData(45.1, t(7, 0), metadata = fitnessMeta)
            ) else emptyList(),
            distance = if (include(HealthDataType.DISTANCE.name)) listOf(
                DistanceData(2100.0, t(8, 0), t(8, 30), fitnessMeta),
                DistanceData(3320.0, t(8, 30), t(9, 0), fitnessMeta),
                DistanceData(5420.0, t(8, 0), t(9, 0), fitnessMeta)
            ) else emptyList(),
            activeCalories = if (include(HealthDataType.ACTIVE_CALORIES.name)) listOf(
                ActiveCaloriesData(95.0, t(8, 0), t(8, 30), fitnessMeta),
                ActiveCaloriesData(217.0, t(8, 30), t(9, 0), fitnessMeta),
                ActiveCaloriesData(312.0, t(8, 0), t(9, 0), fitnessMeta)
            ) else emptyList(),
            totalCalories = if (include(HealthDataType.TOTAL_CALORIES.name)) listOf(
                TotalCaloriesData(2100.0, yesterday, t(10, 30), fitnessMeta)
            ) else emptyList(),
            weight = if (include(HealthDataType.WEIGHT.name)) listOf(
                WeightData(75.5, t(7, 30), fitnessMeta),
                WeightData(75.4, t(7, 0).minus(1, ChronoUnit.DAYS), fitnessMeta)
            ) else emptyList(),
            height = if (include(HealthDataType.HEIGHT.name)) listOf(
                HeightData(1.78, t(7, 30), fitnessMeta)
            ) else emptyList(),
            bloodPressure = if (include(HealthDataType.BLOOD_PRESSURE.name)) listOf(
                BloodPressureData(118.0, 76.0, t(7, 45), fitnessMeta),
                BloodPressureData(120.0, 80.0, t(8, 30), fitnessMeta)
            ) else emptyList(),
            bloodGlucose = if (include(HealthDataType.BLOOD_GLUCOSE.name)) listOf(
                BloodGlucoseData(5.1, t(7, 0), fitnessMeta),
                BloodGlucoseData(5.4, t(8, 0), fitnessMeta),
                BloodGlucoseData(6.2, t(13, 0), fitnessMeta)
            ) else emptyList(),
            oxygenSaturation = if (include(HealthDataType.OXYGEN_SATURATION.name)) listOf(
                OxygenSaturationData(98.0, t(6, 15), metadata = fitnessMeta),
                OxygenSaturationData(98.5, t(6, 30), metadata = fitnessMeta),
                OxygenSaturationData(97.8, t(22, 0), metadata = fitnessMeta)
            ) else emptyList(),
            bodyTemperature = if (include(HealthDataType.BODY_TEMPERATURE.name)) listOf(
                BodyTemperatureData(36.5, t(6, 45), fitnessMeta),
                BodyTemperatureData(36.6, t(7, 0), fitnessMeta)
            ) else emptyList(),
            skinTemperature = if (include(HealthDataType.SKIN_TEMPERATURE.name)) listOf(
                SkinTemperatureData(t(7, 0), 0.1, 36.5, measurementLocation = 3, metadata = fitnessMeta),
                SkinTemperatureData(t(7, 15), 0.15, 36.5, measurementLocation = 3, metadata = fitnessMeta)
            ) else emptyList(),
            respiratoryRate = if (include(HealthDataType.RESPIRATORY_RATE.name)) listOf(
                RespiratoryRateData(13.0, t(6, 20), metadata = fitnessMeta),
                RespiratoryRateData(14.0, t(6, 30), metadata = fitnessMeta),
                RespiratoryRateData(15.0, t(8, 40), metadata = fitnessMeta)
            ) else emptyList(),
            restingHeartRate = if (include(HealthDataType.RESTING_HEART_RATE.name)) listOf(
                RestingHeartRateData(58, t(6, 0), fitnessMeta),
                RestingHeartRateData(57, t(6, 0).minus(1, ChronoUnit.DAYS), fitnessMeta)
            ) else emptyList(),
            exercise = if (include(HealthDataType.EXERCISE.name)) listOf(
                ExerciseData(
                    type = "running",
                    title = "Morning Run",
                    startTime = t(8, 0),
                    endTime = t(9, 0),
                    duration = Duration.ofSeconds(3600),
                    distanceMeters = 5420.0,
                    steps = 6800L,
                    avgCadenceSpm = 170.0,
                    maxCadenceSpm = 182.0,
                    strideLengthMeters = 1.2,
                    metadata = fitnessMeta
                ),
                ExerciseData(
                    type = "walking",
                    title = "Evening Walk",
                    startTime = t(18, 30),
                    endTime = t(19, 10),
                    duration = Duration.ofSeconds(2400),
                    distanceMeters = 2800.0,
                    steps = 3500L,
                    avgCadenceSpm = 110.0,
                    maxCadenceSpm = 125.0,
                    strideLengthMeters = 0.8,
                    metadata = fitnessMeta
                )
            ) else emptyList(),
            hydration = if (include(HealthDataType.HYDRATION.name)) listOf(
                HydrationData(0.35, t(8, 30), t(8, 31), fitnessMeta),
                HydrationData(0.5, t(12, 0), t(12, 1), fitnessMeta),
                HydrationData(0.4, t(16, 0), t(16, 1), fitnessMeta)
            ) else emptyList(),
            nutrition = if (include(HealthDataType.NUTRITION.name)) listOf(
                NutritionData(
                    startTime = t(7, 30),
                    endTime = t(7, 45),
                    calories = 350.0,
                    protein = 25.0,
                    carbs = 45.0,
                    fat = 10.0,
                    sugar = 8.0,
                    sodium = 0.8,
                    dietaryFiber = 5.0,
                    name = "Oatmeal",
                    metadata = fitnessMeta
                ),
                NutritionData(
                    startTime = t(12, 30),
                    endTime = t(13, 0),
                    calories = 620.0,
                    protein = 40.0,
                    carbs = 55.0,
                    fat = 22.0,
                    sugar = 12.0,
                    sodium = 1.4,
                    dietaryFiber = 8.0,
                    name = "Chicken bowl",
                    metadata = fitnessMeta
                )
            ) else emptyList(),
            basalMetabolicRate = if (include(HealthDataType.BASAL_METABOLIC_RATE.name)) listOf(
                BasalMetabolicRateData(85.0, t(6, 0), fitnessMeta)
            ) else emptyList(),
            bodyFat = if (include(HealthDataType.BODY_FAT.name)) listOf(
                BodyFatData(18.5, t(7, 30), fitnessMeta)
            ) else emptyList(),
            leanBodyMass = if (include(HealthDataType.LEAN_BODY_MASS.name)) listOf(
                LeanBodyMassData(61.5, t(7, 30), fitnessMeta)
            ) else emptyList(),
            bodyWaterMass = if (include(HealthDataType.BODY_WATER_MASS.name)) listOf(
                BodyWaterMassData(42.0, t(7, 30), fitnessMeta)
            ) else emptyList(),
            vo2Max = if (include(HealthDataType.VO2_MAX.name)) listOf(
                Vo2MaxData(48.0, t(9, 0), fitnessMeta)
            ) else emptyList(),
            boneMass = if (include(HealthDataType.BONE_MASS.name)) listOf(
                BoneMassData(3.2, t(7, 30), fitnessMeta)
            ) else emptyList(),
            menstruationFlow = if (include(HealthDataType.MENSTRUATION_FLOW.name)) listOf(
                MenstruationFlowData(2, t(8, 0), fitnessMeta)
            ) else emptyList(),
            menstruationPeriod = if (include(HealthDataType.MENSTRUATION_PERIOD.name)) listOf(
                MenstruationPeriodData(yesterday, t(23, 59), fitnessMeta)
            ) else emptyList(),
            intermenstrualBleeding = if (include(HealthDataType.INTERMENSTRUAL_BLEEDING.name)) listOf(
                IntermenstrualBleedingData(t(14, 0), fitnessMeta)
            ) else emptyList(),
            ovulationTest = if (include(HealthDataType.OVULATION_TEST.name)) listOf(
                OvulationTestData(1, t(9, 0), fitnessMeta)
            ) else emptyList(),
            cervicalMucus = if (include(HealthDataType.CERVICAL_MUCUS.name)) listOf(
                CervicalMucusData(3, t(8, 30), fitnessMeta)
            ) else emptyList(),
            sexualActivity = if (include(HealthDataType.SEXUAL_ACTIVITY.name)) listOf(
                SexualActivityData(1, t(22, 0), fitnessMeta)
            ) else emptyList(),
            basalBodyTemperature = if (include(HealthDataType.BASAL_BODY_TEMPERATURE.name)) listOf(
                BasalBodyTemperatureData(36.4, 1, t(6, 30), fitnessMeta)
            ) else emptyList()
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putMetadata(meta: RecordMetadata) {
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
}
