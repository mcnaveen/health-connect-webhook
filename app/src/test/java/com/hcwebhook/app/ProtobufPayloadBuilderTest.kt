package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ProtobufPayloadBuilderTest {

    @Test
    fun build_includesStepsAndMetadata() {
        val now = Instant.parse("2026-05-09T12:00:00Z")
        val data = emptyHealthData().copy(
            steps = listOf(
                StepsData(
                    count = 100,
                    startTime = now.minusSeconds(3600),
                    endTime = now,
                    metadata = RecordMetadata(
                        dataOrigin = "com.example",
                        recordingMethod = "automatically_recorded",
                        deviceManufacturer = "Acme",
                        deviceModel = "Watch",
                        deviceType = 1
                    )
                )
            )
        )

        val payload = ProtobufPayloadBuilder.build(data, appVersion = "9.9.9", timestamp = now)

        assertEquals("2026-05-09T12:00:00Z", payload.timestamp)
        assertEquals("9.9.9", payload.appVersion)
        assertEquals(1, payload.stepsCount)
        assertEquals(100L, payload.getSteps(0).count)
        assertEquals("com.example", payload.getSteps(0).metadata.dataOrigin)
        assertTrue(payload.serializedSize > 0)
    }

    @Test
    fun build_computesBmiWhenWeightAndHeightPresent() {
        val t = Instant.parse("2026-05-09T08:00:00Z")
        val data = emptyHealthData().copy(
            weight = listOf(WeightData(80.0, t)),
            height = listOf(HeightData(1.80, t))
        )

        val payload = ProtobufPayloadBuilder.build(data, "1.0.0", t)
        assertEquals(1, payload.bmiCount)
        assertEquals(80.0 / (1.8 * 1.8), payload.getBmi(0).value, 0.0001)
    }

    @Test
    fun buildTestPayload_isRichLikeJsonMock() {
        val payload = ProtobufPayloadBuilder.buildTestPayload("1.2.3")

        assertEquals("1.2.3", payload.appVersion)
        assertTrue(payload.stepsCount >= 5)
        assertTrue(payload.heartRateCount >= 20)
        assertEquals(1, payload.sleepCount)
        assertEquals(3, payload.getSleep(0).stagesCount)
        assertEquals(2, payload.exerciseCount)
        assertEquals(2, payload.nutritionCount)
        assertTrue(payload.bmiCount >= 1)
        assertTrue(payload.getSteps(0).hasMetadata())
        assertTrue(payload.serializedSize > 500)
    }

    @Test
    fun buildTestPayload_respectsEnabledTypesFilter() {
        val payload = ProtobufPayloadBuilder.buildTestPayload(
            appVersion = "1.0.0",
            enabledTypes = setOf("STEPS", "HEART_RATE")
        )
        assertTrue(payload.stepsCount > 0)
        assertTrue(payload.heartRateCount > 0)
        assertEquals(0, payload.sleepCount)
        assertEquals(0, payload.exerciseCount)
    }

    @Test
    fun parseTarget_httpsAndHostPort() {
        val https = GrpcWebhookClient.parseTarget("https://example.com")
        assertEquals("example.com", https.host)
        assertEquals(443, https.port)
        assertTrue(https.useTls)

        val local = GrpcWebhookClient.parseTarget("192.168.1.10:50051")
        assertEquals("192.168.1.10", local.host)
        assertEquals(50051, local.port)
        assertFalse(local.useTls)

        assertTrue(GrpcWebhookClient.isValidTarget("http://localhost:50051"))
        assertFalse(GrpcWebhookClient.isValidTarget(""))
    }

    private fun emptyHealthData() = HealthData(
        steps = emptyList(),
        sleep = emptyList(),
        heartRate = emptyList(),
        heartRateVariability = emptyList(),
        distance = emptyList(),
        activeCalories = emptyList(),
        totalCalories = emptyList(),
        weight = emptyList(),
        height = emptyList(),
        bloodPressure = emptyList(),
        bloodGlucose = emptyList(),
        oxygenSaturation = emptyList(),
        bodyTemperature = emptyList(),
        skinTemperature = emptyList(),
        respiratoryRate = emptyList(),
        restingHeartRate = emptyList(),
        exercise = emptyList(),
        hydration = emptyList(),
        nutrition = emptyList(),
        basalMetabolicRate = emptyList(),
        bodyFat = emptyList(),
        leanBodyMass = emptyList(),
        bodyWaterMass = emptyList(),
        vo2Max = emptyList(),
        boneMass = emptyList(),
        menstruationFlow = emptyList(),
        menstruationPeriod = emptyList(),
        intermenstrualBleeding = emptyList(),
        ovulationTest = emptyList(),
        cervicalMucus = emptyList(),
        sexualActivity = emptyList(),
        basalBodyTemperature = emptyList()
    )
}
