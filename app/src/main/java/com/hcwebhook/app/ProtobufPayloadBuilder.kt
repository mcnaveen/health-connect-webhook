package com.hcwebhook.app

import com.hcwebhook.app.proto.v1.BasalBodyTemperatureRecord
import com.hcwebhook.app.proto.v1.BasalMetabolicRateRecord
import com.hcwebhook.app.proto.v1.BloodGlucoseRecord
import com.hcwebhook.app.proto.v1.BloodPressureRecord
import com.hcwebhook.app.proto.v1.BmiRecord
import com.hcwebhook.app.proto.v1.BodyFatRecord
import com.hcwebhook.app.proto.v1.CaloriesRecord
import com.hcwebhook.app.proto.v1.CervicalMucusRecord
import com.hcwebhook.app.proto.v1.DistanceRecord
import com.hcwebhook.app.proto.v1.ExerciseRecord
import com.hcwebhook.app.proto.v1.HealthPayload
import com.hcwebhook.app.proto.v1.HeartRateRecord
import com.hcwebhook.app.proto.v1.HeartRateVariabilityRecord
import com.hcwebhook.app.proto.v1.HeightRecord
import com.hcwebhook.app.proto.v1.HydrationRecord
import com.hcwebhook.app.proto.v1.IntermenstrualBleedingRecord
import com.hcwebhook.app.proto.v1.MassRecord
import com.hcwebhook.app.proto.v1.MenstruationFlowRecord
import com.hcwebhook.app.proto.v1.MenstruationPeriodRecord
import com.hcwebhook.app.proto.v1.NutritionRecord
import com.hcwebhook.app.proto.v1.OvulationTestRecord
import com.hcwebhook.app.proto.v1.OxygenSaturationRecord
import com.hcwebhook.app.proto.v1.RecordMetadata as ProtoRecordMetadata
import com.hcwebhook.app.proto.v1.RespiratoryRateRecord
import com.hcwebhook.app.proto.v1.RestingHeartRateRecord
import com.hcwebhook.app.proto.v1.SexualActivityRecord
import com.hcwebhook.app.proto.v1.SkinTemperatureRecord
import com.hcwebhook.app.proto.v1.SleepRecord
import com.hcwebhook.app.proto.v1.SleepStage as ProtoSleepStage
import com.hcwebhook.app.proto.v1.StepsRecord
import com.hcwebhook.app.proto.v1.TemperatureRecord
import com.hcwebhook.app.proto.v1.Vo2MaxRecord
import com.hcwebhook.app.proto.v1.WeightRecord
import java.time.Instant

/**
 * Builds the published protobuf HealthPayload from in-memory HealthData.
 * Field shapes match docs/webhook.md / buildJsonPayload.
 */
object ProtobufPayloadBuilder {

    fun build(healthData: HealthData, appVersion: String, timestamp: Instant = Instant.now()): HealthPayload {
        val builder = HealthPayload.newBuilder()
            .setTimestamp(timestamp.toString())
            .setAppVersion(appVersion)

        healthData.steps.forEach { step ->
            builder.addSteps(
                StepsRecord.newBuilder()
                    .setCount(step.count)
                    .setStartTime(step.startTime.toString())
                    .setEndTime(step.endTime.toString())
                    .apply { step.metadata?.let { metadata = it.toProto() } }
                    .build()
            )
        }

        healthData.sleep.forEach { sleep ->
            val sleepBuilder = SleepRecord.newBuilder()
                .setSessionEndTime(sleep.sessionEndTime.toString())
                .setDurationSeconds(sleep.duration.seconds)
            sleep.stages.forEach { stage ->
                sleepBuilder.addStages(
                    ProtoSleepStage.newBuilder()
                        .setStage(stage.stage)
                        .setStartTime(stage.startTime.toString())
                        .setEndTime(stage.endTime.toString())
                        .setDurationSeconds(stage.duration.seconds)
                        .build()
                )
            }
            sleep.metadata?.let { sleepBuilder.metadata = it.toProto() }
            builder.addSleep(sleepBuilder.build())
        }

        healthData.heartRate.forEach { hr ->
            val hrBuilder = HeartRateRecord.newBuilder().setTime(hr.time.toString())
            if (hr.min != null) {
                hrBuilder.setAvg(hr.bpm)
                hr.min?.let { hrBuilder.setMin(it) }
                hr.max?.let { hrBuilder.setMax(it) }
            } else {
                hrBuilder.setBpm(hr.bpm)
            }
            hr.metadata?.let { hrBuilder.metadata = it.toProto() }
            builder.addHeartRate(hrBuilder.build())
        }

        healthData.heartRateVariability.forEach { hrv ->
            val hrvBuilder = HeartRateVariabilityRecord.newBuilder().setTime(hrv.time.toString())
            if (hrv.min != null) {
                hrvBuilder.setAvg(hrv.rmssdMillis)
                hrv.min?.let { hrvBuilder.setMin(it) }
                hrv.max?.let { hrvBuilder.setMax(it) }
            } else {
                hrvBuilder.setRmssdMillis(hrv.rmssdMillis)
            }
            hrv.metadata?.let { hrvBuilder.metadata = it.toProto() }
            builder.addHeartRateVariability(hrvBuilder.build())
        }

        healthData.distance.forEach {
            builder.addDistance(
                DistanceRecord.newBuilder()
                    .setMeters(it.meters)
                    .setStartTime(it.startTime.toString())
                    .setEndTime(it.endTime.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.activeCalories.forEach {
            builder.addActiveCalories(it.toCaloriesRecord())
        }
        healthData.totalCalories.forEach {
            builder.addTotalCalories(
                CaloriesRecord.newBuilder()
                    .setCalories(it.calories)
                    .setStartTime(it.startTime.toString())
                    .setEndTime(it.endTime.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.weight.forEach {
            builder.addWeight(
                WeightRecord.newBuilder()
                    .setKilograms(it.kilograms)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.height.forEach {
            builder.addHeight(
                HeightRecord.newBuilder()
                    .setMeters(it.meters)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.bloodPressure.forEach {
            builder.addBloodPressure(
                BloodPressureRecord.newBuilder()
                    .setSystolic(it.systolic)
                    .setDiastolic(it.diastolic)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.bloodGlucose.forEach {
            builder.addBloodGlucose(
                BloodGlucoseRecord.newBuilder()
                    .setMmolPerLiter(it.mmolPerLiter)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.oxygenSaturation.forEach { o2 ->
            val o2Builder = OxygenSaturationRecord.newBuilder().setTime(o2.time.toString())
            if (o2.min != null) {
                o2Builder.setAvg(o2.percentage)
                o2.min?.let { o2Builder.setMin(it) }
                o2.max?.let { o2Builder.setMax(it) }
            } else {
                o2Builder.setPercentage(o2.percentage)
            }
            o2.metadata?.let { o2Builder.metadata = it.toProto() }
            builder.addOxygenSaturation(o2Builder.build())
        }

        healthData.bodyTemperature.forEach {
            builder.addBodyTemperature(
                TemperatureRecord.newBuilder()
                    .setCelsius(it.celsius)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.skinTemperature.forEach { skin ->
            val skinBuilder = SkinTemperatureRecord.newBuilder()
                .setTime(skin.time.toString())
                .setMeasurementLocation(skin.measurementLocation)
            if (skin.minDeltaCelsius != null) {
                skinBuilder.setAvgDeltaCelsius(skin.deltaCelsius)
                skin.minDeltaCelsius?.let { skinBuilder.setMinDeltaCelsius(it) }
                skin.maxDeltaCelsius?.let { skinBuilder.setMaxDeltaCelsius(it) }
            } else {
                skinBuilder.setDeltaCelsius(skin.deltaCelsius)
            }
            skin.baselineCelsius?.let { skinBuilder.setBaselineCelsius(it) }
            skin.metadata?.let { skinBuilder.metadata = it.toProto() }
            builder.addSkinTemperature(skinBuilder.build())
        }

        healthData.respiratoryRate.forEach { resp ->
            val respBuilder = RespiratoryRateRecord.newBuilder().setTime(resp.time.toString())
            if (resp.min != null) {
                respBuilder.setAvg(resp.rate)
                resp.min?.let { respBuilder.setMin(it) }
                resp.max?.let { respBuilder.setMax(it) }
            } else {
                respBuilder.setRate(resp.rate)
            }
            resp.metadata?.let { respBuilder.metadata = it.toProto() }
            builder.addRespiratoryRate(respBuilder.build())
        }

        healthData.restingHeartRate.forEach {
            builder.addRestingHeartRate(
                RestingHeartRateRecord.newBuilder()
                    .setBpm(it.bpm)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.exercise.forEach {
            val ex = ExerciseRecord.newBuilder()
                .setType(it.type)
                .setStartTime(it.startTime.toString())
                .setEndTime(it.endTime.toString())
                .setDurationSeconds(it.duration.seconds)
            it.title?.let { title -> ex.setTitle(title) }
            it.distanceMeters?.let { d -> ex.setDistanceMeters(d) }
            it.steps?.let { s -> ex.setSteps(s) }
            it.avgCadenceSpm?.let { c -> ex.setAvgCadenceSpm(c) }
            it.maxCadenceSpm?.let { c -> ex.setMaxCadenceSpm(c) }
            it.strideLengthMeters?.let { s -> ex.setStrideLengthM(s) }
            it.metadata?.let { meta -> ex.metadata = meta.toProto() }
            builder.addExercise(ex.build())
        }

        healthData.hydration.forEach {
            builder.addHydration(
                HydrationRecord.newBuilder()
                    .setLiters(it.liters)
                    .setStartTime(it.startTime.toString())
                    .setEndTime(it.endTime.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.nutrition.forEach {
            val n = NutritionRecord.newBuilder()
                .setStartTime(it.startTime.toString())
                .setEndTime(it.endTime.toString())
            it.calories?.let { c -> n.setCalories(c) }
            it.protein?.let { p -> n.setProteinGrams(p) }
            it.carbs?.let { c -> n.setCarbsGrams(c) }
            it.fat?.let { f -> n.setFatGrams(f) }
            it.sugar?.let { s -> n.setSugarGrams(s) }
            it.sodium?.let { s -> n.setSodiumGrams(s) }
            it.dietaryFiber?.let { f -> n.setDietaryFiberGrams(f) }
            it.name?.let { name -> n.setName(name) }
            it.metadata?.let { meta -> n.metadata = meta.toProto() }
            builder.addNutrition(n.build())
        }

        healthData.basalMetabolicRate.forEach {
            builder.addBasalMetabolicRate(
                BasalMetabolicRateRecord.newBuilder()
                    .setWatts(it.watts)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.bodyFat.forEach {
            builder.addBodyFat(
                BodyFatRecord.newBuilder()
                    .setPercentage(it.percentage)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.leanBodyMass.forEach {
            builder.addLeanBodyMass(it.toMassRecord())
        }
        healthData.bodyWaterMass.forEach {
            builder.addBodyWaterMass(
                MassRecord.newBuilder()
                    .setKilograms(it.kilograms)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        if (healthData.weight.isNotEmpty() && healthData.height.isNotEmpty()) {
            computeBmiEntries(healthData.weight, healthData.height).forEach { entry ->
                builder.addBmi(
                    BmiRecord.newBuilder()
                        .setValue(entry.value)
                        .setTime(entry.time.toString())
                        .setWeightKg(entry.weightKg)
                        .setHeightMeters(entry.heightMeters)
                        .build()
                )
            }
        }

        healthData.vo2Max.forEach {
            builder.addVo2Max(
                Vo2MaxRecord.newBuilder()
                    .setMlPerKgPerMin(it.mlPerKgPerMin)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.boneMass.forEach {
            builder.addBoneMass(
                MassRecord.newBuilder()
                    .setKilograms(it.kilograms)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.menstruationFlow.forEach {
            builder.addMenstruationFlow(
                MenstruationFlowRecord.newBuilder()
                    .setFlow(it.flow)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.menstruationPeriod.forEach {
            builder.addMenstruationPeriod(
                MenstruationPeriodRecord.newBuilder()
                    .setStartTime(it.startTime.toString())
                    .setEndTime(it.endTime.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.intermenstrualBleeding.forEach {
            builder.addIntermenstrualBleeding(
                IntermenstrualBleedingRecord.newBuilder()
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.ovulationTest.forEach {
            builder.addOvulationTest(
                OvulationTestRecord.newBuilder()
                    .setResult(it.result)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.cervicalMucus.forEach {
            builder.addCervicalMucus(
                CervicalMucusRecord.newBuilder()
                    .setAppearance(it.appearance)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.sexualActivity.forEach {
            builder.addSexualActivity(
                SexualActivityRecord.newBuilder()
                    .setProtectionUsed(it.protectionUsed)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.basalBodyTemperature.forEach {
            builder.addBasalBodyTemperature(
                BasalBodyTemperatureRecord.newBuilder()
                    .setCelsius(it.celsius)
                    .setMeasurementLocation(it.measurementLocation)
                    .setTime(it.time.toString())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        return builder.build()
    }

    /**
     * Rich mock payload for webhook **Test** when delivery format is gRPC.
     * Uses the same [MockPayloadBuilder.buildHealthData] source as JSON Test.
     */
    fun buildTestPayload(appVersion: String, enabledTypes: Set<String>? = null): HealthPayload {
        return build(MockPayloadBuilder.buildHealthData(enabledTypes), appVersion)
    }

    private fun ActiveCaloriesData.toCaloriesRecord(): CaloriesRecord {
        val sourceMeta = metadata
        return CaloriesRecord.newBuilder()
            .setCalories(calories)
            .setStartTime(startTime.toString())
            .setEndTime(endTime.toString())
            .apply { sourceMeta?.let { this.metadata = it.toProto() } }
            .build()
    }

    private fun LeanBodyMassData.toMassRecord(): MassRecord {
        val sourceMeta = metadata
        return MassRecord.newBuilder()
            .setKilograms(kilograms)
            .setTime(time.toString())
            .apply { sourceMeta?.let { this.metadata = it.toProto() } }
            .build()
    }
    private fun RecordMetadata.toProto(): ProtoRecordMetadata {
        val b = ProtoRecordMetadata.newBuilder()
            .setDataOrigin(dataOrigin)
            .setRecordingMethod(recordingMethod)
        deviceManufacturer?.let { b.setDeviceManufacturer(it) }
        deviceModel?.let { b.setDeviceModel(it) }
        deviceType?.let { b.setDeviceType(it) }
        return b.build()
    }

    private data class BmiEntry(
        val value: Double,
        val time: Instant,
        val weightKg: Double,
        val heightMeters: Double
    )

    private fun computeBmiEntries(
        weights: List<WeightData>,
        heights: List<HeightData>
    ): List<BmiEntry> {
        if (weights.isEmpty() || heights.isEmpty()) return emptyList()
        return weights.mapNotNull { weight ->
            val height = heights.minByOrNull { h ->
                kotlin.math.abs(h.time.epochSecond - weight.time.epochSecond)
            } ?: return@mapNotNull null
            if (height.meters <= 0.0) return@mapNotNull null
            val value = weight.kilograms / (height.meters * height.meters)
            BmiEntry(value, weight.time, weight.kilograms, height.meters)
        }
    }
}
