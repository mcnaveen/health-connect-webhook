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

/** What Health Connect's aggregate gives over a session's or a lap's window; null when not enabled or no data. */
data class ExerciseStats(
    val distanceMeters: Double? = null,
    val steps: Long? = null,
    val avgCadenceSpm: Double? = null,
    val maxCadenceSpm: Double? = null,
    val strideLengthMeters: Double? = null,
    val avgHeartRateBpm: Long? = null,
    val maxHeartRateBpm: Long? = null
)

/** A lap of an exercise session (`ExerciseSessionRecord.laps`), with what was measured over its window. */
data class ExerciseLapData(
    val startTime: Instant,
    val endTime: Instant,
    val lengthMeters: Double? = null,
    val stats: ExerciseStats = ExerciseStats()
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

fun ExerciseLap.toLapData(stats: ExerciseStats = ExerciseStats()) =
    ExerciseLapData(startTime, endTime, length?.inMeters, stats)

fun ExerciseSegment.toSegmentData() =
    ExerciseSegmentData(startTime, endTime, segmentType.toString(), repetitions)

// --- To the JSON payload ---

/** An exercise's heart rate, `laps` and `segments` — each left out when the session has none. */
fun JsonObjectBuilder.putLapsAndSegments(exercise: ExerciseData, time: PayloadTime) {
    putIfPresent("avg_heart_rate_bpm", exercise.avgHeartRateBpm)
    putIfPresent("max_heart_rate_bpm", exercise.maxHeartRateBpm)
    putArrayUnlessEmpty("laps", exercise.laps) { it.toJson(time) }
    putArrayUnlessEmpty("segments", exercise.segments) { it.toJson(time) }
}

fun ExerciseLapData.toJson(time: PayloadTime): JsonObject = buildJsonObject {
    putWindow(startTime, endTime, time)
    putIfPresent("length_meters", lengthMeters)
    putStats(stats)
}

fun ExerciseSegmentData.toJson(time: PayloadTime): JsonObject = buildJsonObject {
    putWindow(startTime, endTime, time)
    put("segment_type", segmentType)
    put("repetitions", repetitions)
}

private fun JsonObjectBuilder.putStats(stats: ExerciseStats) {
    putIfPresent("distance_meters", stats.distanceMeters)
    putIfPresent("steps", stats.steps)
    putIfPresent("avg_cadence_spm", stats.avgCadenceSpm)
    putIfPresent("max_cadence_spm", stats.maxCadenceSpm)
    putIfPresent("stride_length_m", stats.strideLengthMeters)
    putIfPresent("avg_heart_rate_bpm", stats.avgHeartRateBpm)
    putIfPresent("max_heart_rate_bpm", stats.maxHeartRateBpm)
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
