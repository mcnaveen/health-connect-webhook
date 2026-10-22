package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

class PayloadChunksTest {

    private val t0 = Instant.parse("2026-10-22T00:00:00Z")

    private fun heartRate(n: Int) = List(n) { HeartRateData(bpm = 60L + it, time = t0.plusSeconds(it * 60L)) }
    private fun steps(n: Int) = List(n) { StepsData(count = it.toLong(), startTime = t0, endTime = t0.plusSeconds(60)) }
    private fun deleted(n: Int) = List(n) { DeletedRecord(HealthDataType.HEART_RATE, "gone-$it") }

    @Test
    fun dataThatFits_staysOnePayload() {
        val data = emptyHealthData().copy(steps = steps(3), deleted = deleted(2))

        val chunks = data.chunked(5)

        assertEquals(1, chunks.size)
        assertSame(data, chunks.single())
    }

    @Test
    fun largeData_splitsIntoFullPartsWithNothingLostOrRepeated() {
        val data = emptyHealthData().copy(steps = steps(4), heartRate = heartRate(7), deleted = deleted(5))

        val chunks = data.chunked(5)

        assertEquals(listOf(5, 5, 5, 1), chunks.map { it.itemCount() })
        assertEquals(data.steps, chunks.flatMap { it.steps })
        assertEquals(data.heartRate, chunks.flatMap { it.heartRate })
        assertEquals(data.deleted, chunks.flatMap { it.deleted })
    }

    @Test
    fun itemCount_includesDeletions() {
        assertEquals(6, emptyHealthData().copy(steps = steps(2), heartRate = heartRate(1), deleted = deleted(3)).itemCount())
    }

    @Test
    fun deletionOfARecordInThePayload_isDropped() {
        val meta = RecordMetadata(dataOrigin = "com.fitbit.FitbitMobile", recordingMethod = "automatic", id = "rewritten")
        val data = emptyHealthData().copy(
            heartRate = listOf(HeartRateData(bpm = 70, time = t0, metadata = meta)),
            deleted = listOf(DeletedRecord(HealthDataType.HEART_RATE, "rewritten"), DeletedRecord(HealthDataType.HEART_RATE, "gone")),
        )

        assertEquals(listOf("gone"), data.withoutDeletionsOfPresentRecords().deleted.map { it.id })
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
