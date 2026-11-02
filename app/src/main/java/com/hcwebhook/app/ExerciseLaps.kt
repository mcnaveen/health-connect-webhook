package com.hcwebhook.app

import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/** A lap of an exercise session (`ExerciseSessionRecord.laps`). */
data class ExerciseLapData(
    val startTime: Instant,
    val endTime: Instant,
    val lengthMeters: Double? = null
)

/** A segment of an exercise session (`ExerciseSessionRecord.segments`): an interval, a set, a pool length. */
data class ExerciseSegmentData(
    val startTime: Instant,
    val endTime: Instant,
    /** Health Connect's segment type, in string form like the session's `type`. */
    val segmentType: String,
    val repetitions: Int = 0
)

// --- From Health Connect ---

fun ExerciseLap.toLapData() = ExerciseLapData(startTime, endTime, length?.inMeters)

fun ExerciseSegment.toSegmentData() =
    ExerciseSegmentData(startTime, endTime, segmentType.toString(), repetitions)

// --- To the JSON payload ---

/** An exercise's `laps` and `segments` — each left out when the session has none. */
fun JsonObjectBuilder.putLapsAndSegments(exercise: ExerciseData, time: PayloadTime) {
    putArrayUnlessEmpty("laps", exercise.laps) { it.toJson(time) }
    putArrayUnlessEmpty("segments", exercise.segments) { it.toJson(time) }
}

fun ExerciseLapData.toJson(time: PayloadTime): JsonObject = buildJsonObject {
    putWindow(startTime, endTime, time)
    putIfPresent("length_meters", lengthMeters)
}

fun ExerciseSegmentData.toJson(time: PayloadTime): JsonObject = buildJsonObject {
    putWindow(startTime, endTime, time)
    put("segment_type", segmentType)
    put("repetitions", repetitions)
}

private fun JsonObjectBuilder.putWindow(start: Instant, end: Instant, time: PayloadTime) {
    put("start_time", time.format(start))
    put("end_time", time.format(end))
}

/** Unlike `put(key, Number?)`, which writes `null`, leaves the key out. */
private fun JsonObjectBuilder.putIfPresent(key: String, value: Number?) {
    if (value != null) put(key, value)
}

private fun <T> JsonObjectBuilder.putArrayUnlessEmpty(key: String, items: List<T>, toJson: (T) -> JsonElement) {
    if (items.isNotEmpty()) put(key, JsonArray(items.map(toJson)))
}
