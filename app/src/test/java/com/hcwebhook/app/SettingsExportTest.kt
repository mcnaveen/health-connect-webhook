package com.hcwebhook.app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsExportTest {

    @Test
    fun timestampStyle_survivesRoundTrip() {
        val json = Json.encodeToString(SettingsExport(timestampStyle = TimestampStyle.UTC.name))

        assertEquals("UTC", Json.decodeFromString<SettingsExport>(json).timestampStyle)
    }

    @Test
    fun olderBackup_hasNoTimestampStyle() {
        val export = Json.decodeFromString<SettingsExport>("""{"appVersion":1,"syncIntervalMinutes":720}""")

        assertNull(export.timestampStyle)
    }
}
