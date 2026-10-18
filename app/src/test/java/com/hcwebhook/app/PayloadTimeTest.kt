package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PayloadTimeTest {

    private val instant = Instant.parse("2026-10-18T03:49:19.055Z")
    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test
    fun utc_keepsInstantFormat() {
        val time = PayloadTime(TimestampStyle.UTC, kolkata)
        assertEquals("2026-10-18T03:49:19.055Z", time.format(instant))
        assertEquals("UTC", time.timezoneName)
    }

    @Test
    fun local_writesOffset_andZoneName() {
        val time = PayloadTime(TimestampStyle.LOCAL, kolkata)
        assertEquals("2026-10-18T09:19:19.055+05:30", time.format(instant))
        assertEquals("Asia/Kolkata", time.timezoneName)
    }

    @Test
    fun local_keepsZeroSeconds() {
        // OffsetDateTime.toString() would drop ":00" here.
        val time = PayloadTime(TimestampStyle.LOCAL, kolkata)
        assertEquals("2026-10-18T05:30:00+05:30", time.format(Instant.parse("2026-10-18T00:00:00Z")))
    }

    @Test
    fun local_sameInstantAsUtc() {
        val local = PayloadTime(TimestampStyle.LOCAL, kolkata).format(instant)
        assertEquals(instant, java.time.OffsetDateTime.parse(local).toInstant())
    }
}
