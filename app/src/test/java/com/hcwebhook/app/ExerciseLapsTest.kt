package com.hcwebhook.app

import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.units.Length
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ExerciseLapsTest {

    private val start = Instant.parse("2026-11-02T06:00:00Z")
    private fun at(minutes: Long) = start.plusSeconds(minutes * 60)

    private fun run(
        laps: List<ExerciseLapData> = emptyList(),
        segments: List<ExerciseSegmentData> = emptyList()
    ) = ExerciseData(
        type = "56",
        startTime = start,
        endTime = at(30),
        duration = Duration.ofMinutes(30),
        laps = laps,
        segments = segments
    )

    @Test
    fun healthConnectLapsAndSegments_keepTheirFields() {
        val lap = ExerciseLap(at(0), at(5), Length.meters(1000.0)).toLapData()
        assertEquals(ExerciseLapData(at(0), at(5), 1000.0), lap)
        assertEquals(null, ExerciseLap(at(5), at(10)).toLapData().lengthMeters)

        val segment = ExerciseSegment(
            at(0), at(25), ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, repetitions = 0
        ).toSegmentData()
        assertEquals(ExerciseSegmentData(at(0), at(25), "46", 0), segment)
    }

    @Test
    fun json_hasLapsAndSegmentsInOrder() {
        val json = buildJsonObject {
            putLapsAndSegments(
                run(
                    laps = listOf(
                        ExerciseLapData(at(0), at(5), 1000.0),
                        ExerciseLapData(at(5), at(10))
                    ),
                    segments = listOf(ExerciseSegmentData(at(0), at(25), "46", 12))
                ),
                PayloadTime.UTC
            )
        }

        val laps = json["laps"]!!.jsonArray
        assertEquals(2, laps.size)
        assertEquals("2026-11-02T06:00:00Z", laps[0].jsonObject["start_time"]!!.jsonPrimitive.content)
        assertEquals("2026-11-02T06:05:00Z", laps[0].jsonObject["end_time"]!!.jsonPrimitive.content)
        assertEquals(1000.0, laps[0].jsonObject["length_meters"]!!.jsonPrimitive.double, 0.0)
        assertFalse("length_meters" in laps[1].jsonObject)

        val segment = json["segments"]!!.jsonArray.single().jsonObject
        assertEquals("46", segment["segment_type"]!!.jsonPrimitive.content)
        assertEquals(12, segment["repetitions"]!!.jsonPrimitive.int)
        assertEquals("2026-11-02T06:25:00Z", segment["end_time"]!!.jsonPrimitive.content)
    }

    @Test
    fun json_leavesBothOutWhenTheSessionHasNone() {
        val json = buildJsonObject { putLapsAndSegments(run(), PayloadTime.UTC) }
        assertTrue(json.isEmpty())
    }

    @Test
    fun jsonTestPayload_showsTheRunsLaps() {
        val payload = Json.parseToJsonElement(MockPayloadBuilder.build(setOf("EXERCISE"))).jsonObject
        val exercise = payload["exercise"]!!.jsonArray
        assertEquals(5, exercise[0].jsonObject["laps"]!!.jsonArray.size)
        assertEquals(2, exercise[0].jsonObject["segments"]!!.jsonArray.size)
        assertFalse("laps" in exercise[1].jsonObject)
    }
}
