package com.hcwebhook.app

import java.time.Duration
import java.time.Instant

/**
 * Rules for incremental (watermark) sync.
 *
 * A record is "new" when Health Connect stored or changed it after the previous successful
 * sync — judged by the record's `metadata.lastModifiedTime`, not by when the measurement
 * happened. Comparing measurement times breaks as soon as two apps write the same type with
 * different delays: a phone step counter writes in near real time and pushes the watermark
 * to "now", while a watch companion (e.g. Gadgetbridge) writes minutes that already passed
 * in a batch after each watch sync. Those batches end before the watermark and used to be
 * skipped forever.
 *
 * The watermark is the time the last successful incremental read started, minus
 * [CURSOR_OVERLAP]. A write that Health Connect stamped just before the read but committed
 * just after it is then picked up by the next sync instead of being lost; the price is that
 * such a record may be delivered twice.
 */
object IncrementalSync {

    val CURSOR_OVERLAP: Duration = Duration.ofSeconds(5)

    /** True when a record last modified at [lastModifiedTime] must be sent after [cursor]. */
    fun isNew(lastModifiedTime: Instant, cursor: Instant?): Boolean =
        cursor == null || !lastModifiedTime.isBefore(cursor)

    /**
     * Watermark to store after a delivered sync whose read started at [readStartedAt], or
     * null to keep the stored one. Explicit-range syncs (manual "past N days", custom dates,
     * local API) read a window of their own choosing, so they must not move the incremental
     * watermark: a custom range in the past used to move it backwards, and any range shorter
     * than the background window would skip late writes outside it.
     */
    fun nextCursor(readStartedAt: Instant, explicitRange: Boolean): Instant? =
        if (explicitRange) null else readStartedAt.minus(CURSOR_OVERLAP)
}
