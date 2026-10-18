package com.hcwebhook.app

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** How times are written in the JSON payload. */
enum class TimestampStyle { UTC, LOCAL }

/**
 * Writes every time in the JSON payload in one style, and names the zone for the top-level
 * "timezone" field. UTC keeps the old format (`2026-10-18T03:49:19.055Z`). LOCAL writes the
 * same instant with the device offset (`2026-10-18T09:19:19.055+05:30`).
 */
class PayloadTime(val style: TimestampStyle, private val zone: ZoneId = ZoneId.systemDefault()) {

    /** "UTC", or the device zone such as "Asia/Kolkata". */
    val timezoneName: String = if (style == TimestampStyle.UTC) "UTC" else zone.id

    fun format(instant: Instant): String = when (style) {
        TimestampStyle.UTC -> instant.toString()
        TimestampStyle.LOCAL -> LOCAL_FORMAT.format(instant.atZone(zone).toOffsetDateTime())
    }

    companion object {
        /** Always writes seconds, like Instant.toString(); OffsetDateTime.toString() drops ":00". */
        private val LOCAL_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        val UTC = PayloadTime(TimestampStyle.UTC, ZoneOffset.UTC)
    }
}
