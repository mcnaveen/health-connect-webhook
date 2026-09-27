package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class IncrementalSyncTest {

    private fun at(time: String): Instant = Instant.parse("2026-09-26T${time}Z")

    /** A record as the sync filter sees it: when it was measured and when it was written. */
    private data class Rec(val source: String, val end: Instant, val written: Instant)

    /**
     * Replays background syncs over a store that apps write to at [Rec.written]:
     * each sync reads what is already stored, keeps what [IncrementalSync.isNew] accepts
     * and moves the cursor like SyncManager does after a successful delivery.
     */
    private fun replay(store: List<Rec>, syncTimes: List<Instant>): List<Rec> {
        var cursor: Instant? = null
        val delivered = mutableListOf<Rec>()
        for (syncAt in syncTimes) {
            val batch = store.filter { !it.written.isAfter(syncAt) && IncrementalSync.isNew(it.written, cursor) }
            delivered += batch
            if (batch.isNotEmpty()) cursor = IncrementalSync.nextCursor(syncAt, explicitRange = false)
        }
        return delivered
    }

    @Test
    fun firstSyncTakesEverything() {
        assertTrue(IncrementalSync.isNew(at("00:00:00"), null))
    }

    @Test
    fun recordWrittenAtCursorIsNew() {
        assertTrue(IncrementalSync.isNew(at("12:00:00"), at("12:00:00")))
        assertFalse(IncrementalSync.isNew(at("11:59:59"), at("12:00:00")))
    }

    @Test
    fun cursorTrailsReadStartByOverlap() {
        val readStartedAt = at("12:15:00")
        assertEquals(
            readStartedAt.minus(IncrementalSync.CURSOR_OVERLAP),
            IncrementalSync.nextCursor(readStartedAt, explicitRange = false),
        )
    }

    @Test
    fun explicitRangeKeepsStoredCursor() {
        assertNull(IncrementalSync.nextCursor(at("12:15:00"), explicitRange = true))
    }

    /**
     * The reported case: the phone's step counter writes in near real time, the watch app
     * writes minutes that already passed in a batch after it syncs with the watch. Judged by
     * measurement time the batch ends before what was already sent, and was lost for good.
     */
    @Test
    fun lateBatchFromAnotherAppIsDelivered() {
        val phone = (0 until 4).map { i ->
            val end = at("12:14:30").plusSeconds(900L * i)
            Rec("phone", end, end.plusSeconds(5))
        }
        val watch = (1..14).map { m ->
            Rec("watch", at("12:00:00").plusSeconds(60L * m), at("12:20:00"))
        }
        val syncs = listOf(at("12:15:00"), at("12:30:00"), at("12:45:00"), at("13:00:00"))

        val firstSync = replay(phone + watch, syncs.take(1))
        assertEquals(listOf(phone[0]), firstSync)
        assertTrue(
            "precondition: an end-time watermark from the first sync would drop every watch minute",
            watch.all { it.end.isBefore(phone[0].end) },
        )

        val delivered = replay(phone + watch, syncs)
        assertEquals((phone + watch).toSet(), delivered.toSet())
        assertEquals(delivered.size, delivered.toSet().size)
    }

    @Test
    fun writeJustBeforeReadStartIsDeliveredAgainNotLost() {
        val readStartedAt = at("12:15:00")
        val cursor = IncrementalSync.nextCursor(readStartedAt, explicitRange = false)
        val justBefore = readStartedAt.minusSeconds(1)
        assertTrue(IncrementalSync.isNew(justBefore, cursor))
        assertFalse(IncrementalSync.isNew(readStartedAt.minus(IncrementalSync.CURSOR_OVERLAP).minusSeconds(1), cursor))
    }

    @Test
    fun nothingIsResentOnceSyncsMovePastIt() {
        val rec = Rec("watch", at("12:05:00"), at("12:06:00"))
        val delivered = replay(listOf(rec), listOf(at("12:15:00"), at("12:30:00"), at("12:45:00")))
        assertEquals(listOf(rec), delivered)
    }
}
