package com.hcwebhook.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Simulated-data read/write validation for the `sleep_updated_at` field:
 * the JSON payload must carry the sleep record's last-updated time as ISO 8601,
 * and must keep working (field omitted) when the value is unavailable.
 */
class SleepUpdatedAtJsonTest {

    @Test
    fun sleepPayload_includesSleepUpdatedAtIso8601() {
        val t = Instant.parse("2026-05-09T12:00:00Z")
        val data = emptyHealthData().copy(
            sleep = listOf(
                SleepData(
                    sessionEndTime = t,
                    duration = Duration.ofSeconds(27000),
                    stages = listOf(
                        SleepStage("deep", t.minusSeconds(27000), t.minusSeconds(19800), Duration.ofSeconds(7200))
                    ),
                    sessionUpdatedAt = t.plusSeconds(300)
                )
            )
        )

        val json = SyncManager.buildJsonPayload(data, "1.9.19-test")

        // 新字段:ISO 8601,带 Z(UTC)
        assertTrue(json.contains("\"sleep_updated_at\":\"2026-05-09T12:05:00Z\""))
        // 原有字段兼容,结构未破坏
        assertTrue(json.contains("\"session_end_time\":\"2026-05-09T12:00:00Z\""))
        assertTrue(json.contains("\"duration_seconds\":27000"))
        assertTrue(json.contains("\"stage\":\"deep\""))
    }

    @Test
    fun sleepPayload_omitsSleepUpdatedAtWhenUnavailable() {
        val t = Instant.parse("2026-05-09T12:00:00Z")
        val data = emptyHealthData().copy(
            sleep = listOf(
                SleepData(
                    sessionEndTime = t,
                    duration = Duration.ofSeconds(27000),
                    stages = emptyList()
                )
            )
        )

        val json = SyncManager.buildJsonPayload(data, "1.9.19-test")

        // 无更新时间时整个字段缺省,老解析器完全无感
        assertFalse(json.contains("sleep_updated_at"))
        assertTrue(json.contains("\"duration_seconds\":27000"))
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
