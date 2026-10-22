@file:OptIn(ExperimentalMindfulnessSessionApi::class)

package com.hcwebhook.app

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Changes tokens expire after 30 days, so older edits are rare. */
internal const val MAX_CHANGED_DAYS = 31

/** A record that was deleted from Health Connect after the last sync. */
data class DeletedRecord(
    val type: HealthDataType,
    val id: String,
)

/**
 * Records that Health Connect reports as written or edited since the last sync, but that the
 * `time >= lastSync` filter drops: an edit keeps the original record time, and a late write
 * (e.g. a watch that syncs a day late) has an old time. Each span is the record's
 * (start, end); an instant record has start == end.
 */
data class ChangedRecords(
    val ids: Set<String> = emptySet(),
    val spans: List<Pair<Instant, Instant>> = emptyList(),
    /**
     * Ids of the records the last sync sent at the millisecond of lastSync. lastSync is the
     * time of the newest sent record, so without this list `time >= lastSync` sends that
     * record again on every sync.
     */
    val sentAtLastSync: Set<String> = emptySet(),
) {
    /** True when the record is new since [lastSync] or is a changed record. */
    fun keeps(lastSync: Instant?, time: Instant, id: String?): Boolean =
        lastSync == null || id in ids ||
            (time >= lastSync && !(id in sentAtLastSync && time.toEpochMilli() == lastSync.toEpochMilli()))

    /** True when a changed record overlaps [start, end). */
    fun touches(start: Instant, end: Instant): Boolean =
        spans.any { (spanStart, spanEnd) -> spanStart < end && spanEnd >= start }

    companion object {
        val NONE = ChangedRecords()
    }
}

/** Result of one Changes API read for one data type. */
data class TypeChanges(
    val changed: ChangedRecords,
    val deletedIds: List<String>,
    /** Token to store for the next read; null clears the stored token. */
    val nextToken: String?,
)

/** Deleted records of all types. */
internal fun Map<HealthDataType, TypeChanges>.deletedRecords(): List<DeletedRecord> =
    flatMap { (type, changes) -> changes.deletedIds.map { DeletedRecord(type, it) } }

/** One page of a type's changes feed: the ids it reported as written and as deleted. */
data class ChangesPage(
    val upserted: Set<String>,
    val deleted: List<String>,
)

/**
 * The deletions of a changes feed that still stand, from its [pages] in read order.
 *
 * Health Connect derives a record id from the source app and its client record id. A source
 * that deletes a record and writes it again with the same client record id (Fitbit does this
 * often) gets the same id back, and the feed reports that id as deleted and as written. A
 * page lists its writes before its deletions whatever the real order was, and gives a write
 * as the record is when the page is read, so a deleted record gives no write. A deletion
 * therefore stands only when no page from its own page on reports the same id as written.
 */
internal fun standingDeletions(pages: List<ChangesPage>): List<String> {
    val lastWrittenPage = HashMap<String, Int>()
    pages.forEachIndexed { index, page -> page.upserted.forEach { lastWrittenPage[it] = index } }
    return pages.flatMapIndexed { index, page ->
        page.deleted.filter { id -> (lastWrittenPage[id] ?: -1) < index }
    }.distinct()
}

/**
 * True when the normal incremental read drops a changed record: it starts before the read
 * window, or it ends before [lastSync] or in its millisecond (the read keeps only
 * `time >= lastSync`, and skips the records the last sync sent at that millisecond).
 */
internal fun isMissedByIncrementalRead(span: Pair<Instant, Instant>, lastSync: Instant?, windowStart: Instant): Boolean =
    span.first.isBefore(windowStart) || (lastSync != null && span.second.isBefore(lastSync.plusMillis(1)))

/**
 * (start, end) of a record; an instant record has start == end. Health Connect's
 * `InstantRecord` / `IntervalRecord` interfaces are `internal`, so match each type.
 */
internal fun recordSpan(record: Record): Pair<Instant, Instant>? = when (record) {
    is StepsRecord -> record.startTime to record.endTime
    is SleepSessionRecord -> record.startTime to record.endTime
    is HeartRateRecord -> record.startTime to record.endTime
    is HeartRateVariabilityRmssdRecord -> record.time to record.time
    is DistanceRecord -> record.startTime to record.endTime
    is ActiveCaloriesBurnedRecord -> record.startTime to record.endTime
    is TotalCaloriesBurnedRecord -> record.startTime to record.endTime
    is WeightRecord -> record.time to record.time
    is HeightRecord -> record.time to record.time
    is BloodPressureRecord -> record.time to record.time
    is BloodGlucoseRecord -> record.time to record.time
    is OxygenSaturationRecord -> record.time to record.time
    is BodyTemperatureRecord -> record.time to record.time
    is SkinTemperatureRecord -> record.startTime to record.endTime
    is RespiratoryRateRecord -> record.time to record.time
    is RestingHeartRateRecord -> record.time to record.time
    is ExerciseSessionRecord -> record.startTime to record.endTime
    is HydrationRecord -> record.startTime to record.endTime
    is NutritionRecord -> record.startTime to record.endTime
    is BasalMetabolicRateRecord -> record.time to record.time
    is BodyFatRecord -> record.time to record.time
    is LeanBodyMassRecord -> record.time to record.time
    is BodyWaterMassRecord -> record.time to record.time
    is Vo2MaxRecord -> record.time to record.time
    is BoneMassRecord -> record.time to record.time
    is MenstruationFlowRecord -> record.time to record.time
    is MenstruationPeriodRecord -> record.startTime to record.endTime
    is IntermenstrualBleedingRecord -> record.time to record.time
    is OvulationTestRecord -> record.time to record.time
    is CervicalMucusRecord -> record.time to record.time
    is SexualActivityRecord -> record.time to record.time
    is BasalBodyTemperatureRecord -> record.time to record.time
    is MindfulnessSessionRecord -> record.startTime to record.endTime
    else -> null
}

/**
 * Full local days, before [windowStart], that hold a changed record. Consecutive days merge
 * into one window, so a record that crosses midnight stays inside one read. Only the most
 * recent [MAX_CHANGED_DAYS] days are kept, so one big backfill cannot cause a huge read.
 */
internal fun changedDayWindows(
    changed: ChangedRecords,
    windowStart: Instant,
    windowEnd: Instant,
    zone: ZoneId,
): List<Pair<Instant, Instant>> {
    val days = sortedSetOf<LocalDate>()
    for ((spanStart, spanEnd) in changed.spans) {
        if (!spanStart.isBefore(windowStart)) continue
        var day = spanStart.atZone(zone).toLocalDate()
        val lastDay = minOf(spanEnd, windowStart).atZone(zone).toLocalDate()
        while (!day.isAfter(lastDay)) {
            days.add(day)
            day = day.plusDays(1)
        }
    }
    val windows = mutableListOf<Pair<Instant, Instant>>()
    var first: LocalDate? = null
    var last: LocalDate? = null
    for (day in days.toList().takeLast(MAX_CHANGED_DAYS)) {
        if (first != null && last != null && day == last.plusDays(1)) {
            last = day
            continue
        }
        if (first != null && last != null) windows.add(dayWindow(first, last, zone, windowEnd))
        first = day
        last = day
    }
    if (first != null && last != null) windows.add(dayWindow(first, last, zone, windowEnd))
    return windows.filter { (s, e) -> s.isBefore(e) }
}

private fun dayWindow(first: LocalDate, last: LocalDate, zone: ZoneId, windowEnd: Instant): Pair<Instant, Instant> {
    val end = last.plusDays(1).atStartOfDay(zone).toInstant()
    return first.atStartOfDay(zone).toInstant() to minOf(end, windowEnd)
}

/**
 * Items to bucket in an incremental read. A bucket that holds a changed item is rebuilt
 * from all its items, because the total sent before is now wrong. Other buckets keep only
 * items that are new since the last sync, as before.
 */
internal fun <T> bucketItems(
    items: List<T>,
    bucketOf: (T) -> Long,
    isNew: (T) -> Boolean,
    isChanged: (T) -> Boolean,
): List<T> {
    val changedBuckets = items.filter(isChanged).map(bucketOf).toSet()
    return items.filter { isNew(it) || bucketOf(it) in changedBuckets }
}
