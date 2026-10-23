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
    fun keeps_skipsOnlyRecordsSentAtLastSyncMillisecond() {
        val lastSync = at("2026-05-09T12:00:00Z")
        val inLastSyncMs = lastSync.plusNanos(152_563)
        val changed = ChangedRecords(sentAtLastSync = setOf("sent"))

        assertFalse(changed.keeps(lastSync, lastSync, "sent"))
        assertFalse(changed.keeps(lastSync, inLastSyncMs, "sent"))
        // A different record at the same instant is new.
        assertTrue(changed.keeps(lastSync, lastSync, "other"))
        assertTrue(changed.keeps(lastSync, lastSync.plusMillis(1), "sent"))
        // An edit of the sent record comes in as a changed id.
        assertTrue(changed.copy(ids = setOf("sent")).keeps(lastSync, lastSync, "sent"))
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
        // Record that ends in the millisecond of lastSync: the next read skips it if it was sent.
        val boundary = at("2026-05-09T08:00:00Z") to lastSync.plusNanos(400_000)
        assertTrue(isMissedByIncrementalRead(boundary, lastSync, windowStart))
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
    fun bucketItems_sendsWholeBucketWhenPartIsNew() {
        data class Item(val bucket: Long, val isNew: Boolean, val id: String)
        // 10:00-11:00 bucket: the first half was sent before, the second half is new.
        val items = listOf(
            Item(10, false, "first-half"),
            Item(10, true, "second-half"),
            Item(9, false, "older-hour"),
        )

        val kept = bucketItems(items, { it.bucket }, { it.isNew }, { false })

        assertEquals(listOf("first-half", "second-half"), kept.map { it.id })
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

    @Test
    fun standingDeletions_dropsRecordsWrittenAgainWithTheSameId() {
        val pages = listOf(
            ChangesPage(upserted = setOf("rewritten-later"), deleted = listOf("gone", "rewritten-same-page", "rewritten-later", "deleted-after-write")),
            ChangesPage(upserted = setOf("rewritten-same-page", "rewritten-later"), deleted = emptyList()),
            ChangesPage(upserted = emptySet(), deleted = listOf("gone", "deleted-after-write")),
        )

        // "deleted-after-write" was written on no page; "gone" is reported twice but sent once.
        assertEquals(listOf("gone", "deleted-after-write"), standingDeletions(pages))
    }

    @Test
    fun standingDeletions_keepsDeletionAfterAnEarlierWrite() {
        val pages = listOf(
            ChangesPage(upserted = setOf("a"), deleted = emptyList()),
            ChangesPage(upserted = emptySet(), deleted = listOf("a")),
        )

        assertEquals(listOf("a"), standingDeletions(pages))
    }

    @Test
    fun deletedRecords_listsEveryType() {
        val changes = mapOf(
            HealthDataType.HEART_RATE to TypeChanges(ChangedRecords.NONE, listOf("a", "b"), "hr-next"),
            HealthDataType.STEPS to TypeChanges(ChangedRecords.NONE, listOf("c"), "steps-next"),
        )

        assertEquals(
            listOf(
                DeletedRecord(HealthDataType.HEART_RATE, "a"),
                DeletedRecord(HealthDataType.HEART_RATE, "b"),
                DeletedRecord(HealthDataType.STEPS, "c"),
            ),
            changes.deletedRecords(),
        )
    }
}
