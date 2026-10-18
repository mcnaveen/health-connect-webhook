package com.hcwebhook.app

import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Optional `?start=&end=` range for the local HTTP server (`GET /`, `POST /sync`).
 *
 * Each value is an ISO-8601 instant (`2026-07-01T00:00:00Z`, or with an offset), a date
 * (`2026-07-01`, local midnight), or epoch milliseconds. A date for `end` is inclusive: it
 * means the end of that day. `start` alone reads up to now.
 */
object LocalHttpRange {

    sealed interface Result {
        /** No start or end: use `days` or the default window. */
        object None : Result
        data class Valid(val start: Instant, val end: Instant) : Result
        data class Invalid(val message: String) : Result
    }

    fun parse(
        params: Map<String, String>,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): Result {
        val rawStart = params["start"]?.takeIf { it.isNotBlank() }
        val rawEnd = params["end"]?.takeIf { it.isNotBlank() }
        if (rawStart == null && rawEnd == null) return Result.None
        if (params.containsKey("days")) return Result.Invalid("Use either days or start/end, not both")
        if (rawStart == null) return Result.Invalid("end needs a start")

        val start = parseValue(rawStart, isEnd = false, zone)
            ?: return Result.Invalid("Cannot parse start: use an ISO-8601 instant, a date (YYYY-MM-DD) or epoch milliseconds")
        val end = if (rawEnd == null) {
            now
        } else {
            parseValue(rawEnd, isEnd = true, zone)
                ?: return Result.Invalid("Cannot parse end: use an ISO-8601 instant, a date (YYYY-MM-DD) or epoch milliseconds")
        }
        if (!start.isBefore(end)) return Result.Invalid("start must be before end")
        return Result.Valid(start, end)
    }

    private fun parseValue(raw: String, isEnd: Boolean, zone: ZoneId): Instant? {
        // Query values are not decoded by the server. Keep a literal "+" (a UTC offset)
        // instead of letting the decoder turn it into a space.
        val value = runCatching { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrNull()?.trim()
            ?: return null
        value.toLongOrNull()?.let { return Instant.ofEpochMilli(it) }
        runCatching { return Instant.parse(value) }
        runCatching { return OffsetDateTime.parse(value).toInstant() }
        return runCatching {
            val date = LocalDate.parse(value)
            (if (isEnd) date.plusDays(1) else date).atStartOfDay(zone).toInstant()
        }.getOrNull()
    }
}
