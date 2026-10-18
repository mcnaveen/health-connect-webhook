package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LocalHttpRangeTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-10-18T06:00:00Z")
    private fun parse(vararg pairs: Pair<String, String>) = LocalHttpRange.parse(mapOf(*pairs), zone, now)

    @Test
    fun none_whenNoStartOrEnd() {
        assertEquals(LocalHttpRange.Result.None, parse())
        assertEquals(LocalHttpRange.Result.None, parse("days" to "7"))
    }

    @Test
    fun dates_areLocalMidnight_endInclusive() {
        assertEquals(
            LocalHttpRange.Result.Valid(Instant.parse("2026-06-30T18:30:00Z"), Instant.parse("2026-07-14T18:30:00Z")),
            parse("start" to "2026-07-01", "end" to "2026-07-14"),
        )
    }

    @Test
    fun instantsOffsetsAndEpochMillis() {
        assertEquals(
            LocalHttpRange.Result.Valid(Instant.parse("2026-07-01T00:00:00Z"), Instant.ofEpochMilli(1_783_000_000_000)),
            parse("start" to "2026-07-01T00:00:00Z", "end" to "1783000000000"),
        )
        // Raw "+" offset and a percent-encoded one both work.
        val expected = Instant.parse("2026-07-01T00:00:00Z")
        assertEquals(expected, (parse("start" to "2026-07-01T05:30:00+05:30") as LocalHttpRange.Result.Valid).start)
        assertEquals(expected, (parse("start" to "2026-07-01T05%3A30%3A00%2B05%3A30") as LocalHttpRange.Result.Valid).start)
    }

    @Test
    fun startAlone_readsUpToNow() {
        assertEquals(now, (parse("start" to "2026-10-01") as LocalHttpRange.Result.Valid).end)
    }

    @Test
    fun invalidInputs_areRejected() {
        assertTrue(parse("start" to "July") is LocalHttpRange.Result.Invalid)
        assertTrue(parse("start" to "2026-07-14", "end" to "2026-07-01") is LocalHttpRange.Result.Invalid)
        assertTrue(parse("end" to "2026-07-14") is LocalHttpRange.Result.Invalid)
        assertTrue(parse("start" to "2026-07-01", "days" to "7") is LocalHttpRange.Result.Invalid)
    }
}
