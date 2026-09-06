package com.hcwebhook.app

import com.google.protobuf.Duration as ProtoDuration
import com.google.protobuf.Timestamp
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
import com.hcwebhook.app.proto.v1.HeartRateAggregate
import com.hcwebhook.app.proto.v1.HeartRateRecord
import com.hcwebhook.app.proto.v1.HeartRateSample
import com.hcwebhook.app.proto.v1.HeartRateVariabilityAggregate
import com.hcwebhook.app.proto.v1.HeartRateVariabilityRecord
import com.hcwebhook.app.proto.v1.HeartRateVariabilitySample
import com.hcwebhook.app.proto.v1.HeightRecord
import com.hcwebhook.app.proto.v1.HydrationRecord
import com.hcwebhook.app.proto.v1.IntermenstrualBleedingRecord
import com.hcwebhook.app.proto.v1.IntervalZoneOffset
import com.hcwebhook.app.proto.v1.MassRecord
import com.hcwebhook.app.proto.v1.MenstruationFlowRecord
import com.hcwebhook.app.proto.v1.MenstruationPeriodRecord
import com.hcwebhook.app.proto.v1.NutritionRecord
import com.hcwebhook.app.proto.v1.OvulationTestRecord
import com.hcwebhook.app.proto.v1.OxygenSaturationAggregate
import com.hcwebhook.app.proto.v1.OxygenSaturationRecord
import com.hcwebhook.app.proto.v1.OxygenSaturationSample
import com.hcwebhook.app.proto.v1.RecordMetadata as ProtoRecordMetadata
import com.hcwebhook.app.proto.v1.RespiratoryRateAggregate
import com.hcwebhook.app.proto.v1.RespiratoryRateRecord
import com.hcwebhook.app.proto.v1.RespiratoryRateSample
import com.hcwebhook.app.proto.v1.RestingHeartRateRecord
import com.hcwebhook.app.proto.v1.SexualActivityRecord
import com.hcwebhook.app.proto.v1.SkinTemperatureAggregate
import com.hcwebhook.app.proto.v1.SkinTemperatureRecord
import com.hcwebhook.app.proto.v1.SkinTemperatureSample
import com.hcwebhook.app.proto.v1.SleepRecord
import com.hcwebhook.app.proto.v1.SleepStage as ProtoSleepStage
import com.hcwebhook.app.proto.v1.StepsRecord
import com.hcwebhook.app.proto.v1.TemperatureRecord
import com.hcwebhook.app.proto.v1.Vo2MaxRecord
import com.hcwebhook.app.proto.v1.WeightRecord
import java.time.Duration
import java.time.Instant

/**
 * Builds the published protobuf HealthPayload from in-memory HealthData.
 * Field shapes match docs/webhook.md / buildJsonPayload, except where the proto
 * schema is deliberately richer: record instants are [Timestamp], durations are
 * [ProtoDuration], and sample-vs-aggregate records use a oneof.
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
                    .setStartTime(step.startTime.toTimestamp())
                    .setEndTime(step.endTime.toTimestamp())
                    .apply { step.metadata?.let { metadata = it.toProto() } }
                    .build()
            )
        }

        healthData.sleep.forEach { sleep ->
            val sleepBuilder = SleepRecord.newBuilder()
                .setSessionEndTime(sleep.sessionEndTime.toTimestamp())
                .setDuration(sleep.duration.toProtoDuration())
            sleep.stages.forEach { stage ->
                sleepBuilder.addStages(
                    ProtoSleepStage.newBuilder()
                        .setStage(stage.stage)
                        .setStartTime(stage.startTime.toTimestamp())
                        .setEndTime(stage.endTime.toTimestamp())
                        .setDuration(stage.duration.toProtoDuration())
                        .build()
                )
            }
            sleep.metadata?.let { sleepBuilder.metadata = it.toProto() }
            builder.addSleep(sleepBuilder.build())
        }

        healthData.heartRate.forEach { hr ->
            val hrBuilder = HeartRateRecord.newBuilder().setTime(hr.time.toTimestamp())
            if (hr.min != null) {
                val agg = HeartRateAggregate.newBuilder().setAvg(hr.bpm).setMin(hr.min)
                hr.max?.let { agg.setMax(it) }
                hrBuilder.setAggregate(agg.build())
            } else {
                hrBuilder.setSample(HeartRateSample.newBuilder().setBpm(hr.bpm).build())
            }
            hr.metadata?.let { hrBuilder.metadata = it.toProto() }
            builder.addHeartRate(hrBuilder.build())
        }

        healthData.heartRateVariability.forEach { hrv ->
            val hrvBuilder = HeartRateVariabilityRecord.newBuilder().setTime(hrv.time.toTimestamp())
            if (hrv.min != null) {
                val agg = HeartRateVariabilityAggregate.newBuilder().setAvg(hrv.rmssdMillis).setMin(hrv.min)
                hrv.max?.let { agg.setMax(it) }
                hrvBuilder.setAggregate(agg.build())
            } else {
                hrvBuilder.setSample(HeartRateVariabilitySample.newBuilder().setRmssdMillis(hrv.rmssdMillis).build())
            }
            hrv.metadata?.let { hrvBuilder.metadata = it.toProto() }
            builder.addHeartRateVariability(hrvBuilder.build())
        }

        healthData.distance.forEach {
            builder.addDistance(
                DistanceRecord.newBuilder()
                    .setMeters(it.meters)
                    .setStartTime(it.startTime.toTimestamp())
                    .setEndTime(it.endTime.toTimestamp())
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
                    .setStartTime(it.startTime.toTimestamp())
                    .setEndTime(it.endTime.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.weight.forEach {
            builder.addWeight(
                WeightRecord.newBuilder()
                    .setKilograms(it.kilograms)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.height.forEach {
            builder.addHeight(
                HeightRecord.newBuilder()
                    .setMeters(it.meters)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.bloodPressure.forEach {
            builder.addBloodPressure(
                BloodPressureRecord.newBuilder()
                    .setSystolic(it.systolic)
                    .setDiastolic(it.diastolic)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.bloodGlucose.forEach {
            builder.addBloodGlucose(
                BloodGlucoseRecord.newBuilder()
                    .setMmolPerLiter(it.mmolPerLiter)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.oxygenSaturation.forEach { o2 ->
            val o2Builder = OxygenSaturationRecord.newBuilder().setTime(o2.time.toTimestamp())
            if (o2.min != null) {
                val agg = OxygenSaturationAggregate.newBuilder().setAvg(o2.percentage).setMin(o2.min)
                o2.max?.let { agg.setMax(it) }
                o2Builder.setAggregate(agg.build())
            } else {
                o2Builder.setSample(OxygenSaturationSample.newBuilder().setPercentage(o2.percentage).build())
            }
            o2.metadata?.let { o2Builder.metadata = it.toProto() }
            builder.addOxygenSaturation(o2Builder.build())
        }

        healthData.bodyTemperature.forEach {
            builder.addBodyTemperature(
                TemperatureRecord.newBuilder()
                    .setCelsius(it.celsius)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.skinTemperature.forEach { skin ->
            val skinBuilder = SkinTemperatureRecord.newBuilder()
                .setTime(skin.time.toTimestamp())
                .setMeasurementLocation(skin.measurementLocation)
            if (skin.minDeltaCelsius != null) {
                val agg = SkinTemperatureAggregate.newBuilder()
                    .setAvgDeltaCelsius(skin.deltaCelsius)
                    .setMinDeltaCelsius(skin.minDeltaCelsius)
                skin.maxDeltaCelsius?.let { agg.setMaxDeltaCelsius(it) }
                skinBuilder.setAggregate(agg.build())
            } else {
                skinBuilder.setSample(SkinTemperatureSample.newBuilder().setDeltaCelsius(skin.deltaCelsius).build())
            }
            skin.baselineCelsius?.let { skinBuilder.setBaselineCelsius(it) }
            skin.metadata?.let { skinBuilder.metadata = it.toProto() }
            builder.addSkinTemperature(skinBuilder.build())
        }

        healthData.respiratoryRate.forEach { resp ->
            val respBuilder = RespiratoryRateRecord.newBuilder().setTime(resp.time.toTimestamp())
            if (resp.min != null) {
                val agg = RespiratoryRateAggregate.newBuilder().setAvg(resp.rate).setMin(resp.min)
                resp.max?.let { agg.setMax(it) }
                respBuilder.setAggregate(agg.build())
            } else {
                respBuilder.setSample(RespiratoryRateSample.newBuilder().setRate(resp.rate).build())
            }
            resp.metadata?.let { respBuilder.metadata = it.toProto() }
            builder.addRespiratoryRate(respBuilder.build())
        }

        healthData.restingHeartRate.forEach {
            builder.addRestingHeartRate(
                RestingHeartRateRecord.newBuilder()
                    .setBpm(it.bpm)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.exercise.forEach {
            val ex = ExerciseRecord.newBuilder()
                .setType(it.type)
                .setStartTime(it.startTime.toTimestamp())
                .setEndTime(it.endTime.toTimestamp())
                .setDuration(it.duration.toProtoDuration())
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
                    .setStartTime(it.startTime.toTimestamp())
                    .setEndTime(it.endTime.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.nutrition.forEach {
            val n = NutritionRecord.newBuilder()
                .setStartTime(it.startTime.toTimestamp())
                .setEndTime(it.endTime.toTimestamp())
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
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.bodyFat.forEach {
            builder.addBodyFat(
                BodyFatRecord.newBuilder()
                    .setPercentage(it.percentage)
                    .setTime(it.time.toTimestamp())
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
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        if (healthData.weight.isNotEmpty() && healthData.height.isNotEmpty()) {
            computeBmiEntries(healthData.weight, healthData.height).forEach { entry ->
                builder.addBmi(
                    BmiRecord.newBuilder()
                        .setValue(entry.value)
                        .setTime(entry.time.toTimestamp())
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
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.boneMass.forEach {
            builder.addBoneMass(
                MassRecord.newBuilder()
                    .setKilograms(it.kilograms)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.menstruationFlow.forEach {
            builder.addMenstruationFlow(
                MenstruationFlowRecord.newBuilder()
                    .setFlow(it.flow)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.menstruationPeriod.forEach {
            builder.addMenstruationPeriod(
                MenstruationPeriodRecord.newBuilder()
                    .setStartTime(it.startTime.toTimestamp())
                    .setEndTime(it.endTime.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.intermenstrualBleeding.forEach {
            builder.addIntermenstrualBleeding(
                IntermenstrualBleedingRecord.newBuilder()
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.ovulationTest.forEach {
            builder.addOvulationTest(
                OvulationTestRecord.newBuilder()
                    .setResult(it.result)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.cervicalMucus.forEach {
            builder.addCervicalMucus(
                CervicalMucusRecord.newBuilder()
                    .setAppearance(it.appearance)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.sexualActivity.forEach {
            builder.addSexualActivity(
                SexualActivityRecord.newBuilder()
                    .setProtectionUsed(it.protectionUsed)
                    .setTime(it.time.toTimestamp())
                    .apply { it.metadata?.let { meta -> metadata = meta.toProto() } }
                    .build()
            )
        }

        healthData.basalBodyTemperature.forEach {
            builder.addBasalBodyTemperature(
                BasalBodyTemperatureRecord.newBuilder()
                    .setCelsius(it.celsius)
                    .setMeasurementLocation(it.measurementLocation)
                    .setTime(it.time.toTimestamp())
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
            .setStartTime(startTime.toTimestamp())
            .setEndTime(endTime.toTimestamp())
            .apply { sourceMeta?.let { this.metadata = it.toProto() } }
            .build()
    }

    private fun LeanBodyMassData.toMassRecord(): MassRecord {
        val sourceMeta = metadata
        return MassRecord.newBuilder()
            .setKilograms(kilograms)
            .setTime(time.toTimestamp())
            .apply { sourceMeta?.let { this.metadata = it.toProto() } }
            .build()
    }

    private fun Instant.toTimestamp(): Timestamp =
        Timestamp.newBuilder().setSeconds(epochSecond).setNanos(nano).build()

    private fun Duration.toProtoDuration(): ProtoDuration =
        ProtoDuration.newBuilder().setSeconds(seconds).setNanos(nano).build()

    private fun RecordMetadata.toProto(): ProtoRecordMetadata {
        val b = ProtoRecordMetadata.newBuilder()
            .setDataOrigin(dataOrigin)
            .setRecordingMethod(recordingMethod)
            .setId(id)
            .setClientRecordVersion(clientRecordVersion)
        deviceManufacturer?.let { b.setDeviceManufacturer(it) }
        deviceModel?.let { b.setDeviceModel(it) }
        deviceType?.let { b.setDeviceType(it) }
        clientRecordId?.let { b.setClientRecordId(it) }
        lastModifiedTime?.let { b.setLastModifiedTime(it.toTimestamp()) }
        if (zoneOffsetSeconds != null) {
            b.setInstantZoneOffsetSeconds(zoneOffsetSeconds)
        } else if (startZoneOffsetSeconds != null || endZoneOffsetSeconds != null) {
            val interval = IntervalZoneOffset.newBuilder()
            startZoneOffsetSeconds?.let { interval.setStartZoneOffsetSeconds(it) }
            endZoneOffsetSeconds?.let { interval.setEndZoneOffsetSeconds(it) }
            b.setIntervalZoneOffset(interval.build())
        }
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
