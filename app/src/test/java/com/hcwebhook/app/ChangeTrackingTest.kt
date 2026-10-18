package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ChangeTrackingTest {

    private val utc = ZoneId.of("UTC")
    private fun at(iso: String) = Instant.parse(iso)

    @Test
    fun keeps_newRecordOrChangedId() {
        val lastSync = at("2026-05-09T12:00:00Z")
        val changed = ChangedRecords(ids = setOf("edited"))

        assertTrue(changed.keeps(lastSync, at("2026-05-09T13:00:00Z"), "any"))
        assertTrue(changed.keeps(lastSync, at("2026-05-09T08:00:00Z"), "edited"))
        assertFalse(changed.keeps(lastSync, at("2026-05-09T08:00:00Z"), "old"))
        assertTrue(changed.keeps(null, at("2026-05-09T08:00:00Z"), null))
    }

    @Test
    fun touches_spanOverlapsHalfOpenWindow() {
        val instant = at("2026-05-09T10:00:00Z")
        val changed = ChangedRecords(spans = listOf(instant to instant))

        assertTrue(changed.touches(at("2026-05-09T00:00:00Z"), at("2026-05-10T00:00:00Z")))
        assertFalse(changed.touches(at("2026-05-08T00:00:00Z"), instant))
        assertTrue(changed.touches(instant, at("2026-05-09T11:00:00Z")))
    }

    @Test
    fun missed_onlyWhenBeforeWindowOrLastSync() {
        val windowStart = at("2026-05-08T00:00:00Z")
        val lastSync = at("2026-05-09T12:00:00Z")

        // Edit inside the window, record ends before lastSync: the time filter drops it.
        val edited = at("2026-05-09T08:00:00Z") to at("2026-05-09T08:30:00Z")
        assertTrue(isMissedByIncrementalRead(edited, lastSync, windowStart))
        // New record after lastSync: the normal read gets it.
        val fresh = at("2026-05-09T13:00:00Z") to at("2026-05-09T13:30:00Z")
        assertFalse(isMissedByIncrementalRead(fresh, lastSync, windowStart))
        // Record before the window: missed even without a lastSync.
        val old = at("2026-05-01T08:00:00Z") to at("2026-05-01T09:00:00Z")
        assertTrue(isMissedByIncrementalRead(old, null, windowStart))
    }

    @Test
    fun changedDayWindows_mergesConsecutiveDaysAndSkipsWindowSpans() {
        val windowStart = at("2026-05-08T12:00:00Z")
        val windowEnd = at("2026-05-10T12:00:00Z")
        val changed = ChangedRecords(
            spans = listOf(
                at("2026-05-01T10:00:00Z") to at("2026-05-01T10:00:00Z"),
                // Crosses midnight: both days in one window.
                at("2026-05-03T22:00:00Z") to at("2026-05-04T06:00:00Z"),
                // Inside the main window: no extra read.
                at("2026-05-09T10:00:00Z") to at("2026-05-09T10:00:00Z"),
            )
        )

        val windows = changedDayWindows(changed, windowStart, windowEnd, utc)

        assertEquals(
            listOf(
                at("2026-05-01T00:00:00Z") to at("2026-05-02T00:00:00Z"),
                at("2026-05-03T00:00:00Z") to at("2026-05-05T00:00:00Z"),
            ),
            windows,
        )
    }

    @Test
    fun changedDayWindows_coversFullDayAtWindowStart() {
        val windowStart = at("2026-05-08T12:00:00Z")
        val changed = ChangedRecords(spans = listOf(at("2026-05-08T09:00:00Z") to at("2026-05-08T09:00:00Z")))

        val windows = changedDayWindows(changed, windowStart, at("2026-05-10T12:00:00Z"), utc)

        // Full local day, so a daily total is not a partial-day value.
        assertEquals(listOf(at("2026-05-08T00:00:00Z") to at("2026-05-09T00:00:00Z")), windows)
    }

    @Test
    fun changedDayWindows_keepsOnlyMostRecentDays() {
        val windowStart = at("2026-06-30T00:00:00Z")
        val spans = (0 until MAX_CHANGED_DAYS + 5).map { i ->
            val t = at("2026-04-01T10:00:00Z").plusSeconds(i * 2L * 86_400L)
            t to t
        }

        val windows = changedDayWindows(ChangedRecords(spans = spans), windowStart, at("2026-07-01T00:00:00Z"), utc)

        assertEquals(MAX_CHANGED_DAYS, windows.size)
        assertEquals(spans.last().first.atZone(utc).toLocalDate(), windows.last().first.atZone(utc).toLocalDate())
    }

    @Test
    fun withoutChangedRecords_keepsDeletionsAndTokens() {
        val t = at("2026-05-01T10:00:00Z")
        val changes = mapOf(
            HealthDataType.HEART_RATE to TypeChanges(ChangedRecords(setOf("hr"), listOf(t to t)), listOf("gone"), "token-2"),
            HealthDataType.WEIGHT to TypeChanges(ChangedRecords.NONE, emptyList(), "token-9"),
        )
        assertTrue(changes.hasChangedRecords())

        val dropped = changes.withoutChangedRecords()

        assertFalse(dropped.hasChangedRecords())
        assertEquals(listOf("gone"), dropped.getValue(HealthDataType.HEART_RATE).deletedIds)
        assertEquals("token-2", dropped.getValue(HealthDataType.HEART_RATE).nextToken)
        assertEquals("token-9", dropped.getValue(HealthDataType.WEIGHT).nextToken)
    }

    @Test
    fun bucketItems_rebuildsOnlyBucketsWithChangedItems() {
        data class Item(val bucket: Long, val isNew: Boolean, val id: String)
        val items = listOf(
            Item(1, false, "a"),
            Item(1, false, "edited"),
            Item(2, false, "b"),
            Item(3, true, "c"),
        )

        val kept = bucketItems(items, { it.bucket }, { it.isNew }, { it.id == "edited" })

        assertEquals(listOf("a", "edited", "c"), kept.map { it.id })
    }
}
