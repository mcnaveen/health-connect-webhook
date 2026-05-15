package com.hcwebhook.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardFormatterTest {

    @Test
    fun formatSteps_returnsWholeNumber() {
        assertEquals("22", DashboardFormatter.formatSteps(22))
        assertEquals("0", DashboardFormatter.formatSteps(0))
    }

    @Test
    fun formatDistanceKm_usesTwoDecimalsForSmallValues() {
        assertEquals("0.02", DashboardFormatter.formatDistanceKm(20.0))
        assertEquals("1.50", DashboardFormatter.formatDistanceKm(1500.0))
    }

    @Test
    fun formatDistanceKm_usesFewerDecimalsForLargeValues() {
        assertEquals("12.3", DashboardFormatter.formatDistanceKm(12_345.0))
        assertEquals("105", DashboardFormatter.formatDistanceKm(105_000.0))
    }

    @Test
    fun formatCalories_truncatesToWholeNumber() {
        assertEquals("0", DashboardFormatter.formatCalories(0.0))
        assertEquals("150", DashboardFormatter.formatCalories(150.9))
    }

    @Test
    fun formatDurationMinutes_formatsHoursAndMinutes() {
        assertEquals("0", DashboardFormatter.formatDurationMinutes(0))
        assertEquals("45m", DashboardFormatter.formatDurationMinutes(45))
        assertEquals("1h 30m", DashboardFormatter.formatDurationMinutes(90))
    }
}
