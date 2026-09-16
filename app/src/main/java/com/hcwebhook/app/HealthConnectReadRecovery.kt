package com.hcwebhook.app

import java.time.Duration
import java.time.Instant

/**
 * Helpers for recovering when Health Connect returns interval records that
 * violate androidx constructors (`startTime` must be strictly before `endTime`).
 *
 * Some companion apps write zero/negative-duration Steps/Distance/Calories/
 * Exercise/Sleep records. A single bad row fails the whole page decode, so
 * reads bisect the window and skip irreducible corrupt slices.
 */
internal object HealthConnectReadRecovery {
    /** Smallest window we will try before skipping a corrupt slice. */
    val MIN_CORRUPT_SKIP_CHUNK: Duration = Duration.ofMinutes(1)

    fun isCorruptIntervalRecordError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val msg = current.message.orEmpty()
            // androidx IntervalRecord / ExerciseSessionRecord / StepsRecord / …
            if (msg.contains("startTime must be before endTime", ignoreCase = true)) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Midpoint strictly between [start] and [end], or null when the window is
     * too small to split further.
     */
    fun midpoint(start: Instant, end: Instant): Instant? {
        if (!start.isBefore(end)) return null
        val duration = Duration.between(start, end)
        if (duration <= MIN_CORRUPT_SKIP_CHUNK) return null
        val mid = start.plus(duration.dividedBy(2))
        return mid.takeIf { start.isBefore(it) && it.isBefore(end) }
    }
}
