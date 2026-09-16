package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HealthConnectReadRecoveryTest {

    @Test
    fun detectsCorruptIntervalRecordMessage() {
        val error = IllegalArgumentException("startTime must be before endTime.")
        assertTrue(HealthConnectReadRecovery.isCorruptIntervalRecordError(error))
    }

    @Test
    fun detectsCorruptIntervalRecordMessageInCauseChain() {
        val root = IllegalArgumentException("startTime must be before endTime.")
        val wrapped = Exception("STEPS: ${root.message}", root)
        assertTrue(HealthConnectReadRecovery.isCorruptIntervalRecordError(wrapped))
    }

    @Test
    fun ignoresUnrelatedErrors() {
        assertFalse(
            HealthConnectReadRecovery.isCorruptIntervalRecordError(
                IllegalArgumentException("end time needs be after start time"),
            ),
        )
        assertFalse(
            HealthConnectReadRecovery.isCorruptIntervalRecordError(
                IllegalStateException("quota exceeded"),
            ),
        )
    }

    @Test
    fun midpointSplitsStrictlyInsideWindow() {
        val start = Instant.parse("2026-09-10T00:00:00Z")
        val end = Instant.parse("2026-09-10T02:00:00Z")
        val mid = HealthConnectReadRecovery.midpoint(start, end)
        assertNotNull(mid)
        assertTrue(start.isBefore(mid!!))
        assertTrue(mid.isBefore(end))
        assertEquals(Duration.ofHours(1), Duration.between(start, mid))
    }

    @Test
    fun midpointReturnsNullForTinyWindow() {
        val start = Instant.parse("2026-09-10T00:00:00Z")
        val end = start.plus(HealthConnectReadRecovery.MIN_CORRUPT_SKIP_CHUNK)
        assertNull(HealthConnectReadRecovery.midpoint(start, end))
    }

    @Test
    fun midpointReturnsNullWhenStartNotBeforeEnd() {
        val t = Instant.parse("2026-09-10T00:00:00Z")
        assertNull(HealthConnectReadRecovery.midpoint(t, t))
        assertNull(HealthConnectReadRecovery.midpoint(t.plusSeconds(1), t))
    }
}
