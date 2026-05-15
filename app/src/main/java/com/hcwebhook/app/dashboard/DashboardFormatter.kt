package com.hcwebhook.app.dashboard

import java.time.Duration
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object DashboardFormatter {

    const val NO_DATA = "—"

    fun formatSteps(steps: Long): String = steps.toString()

    fun formatDistanceKm(distanceMeters: Double): String {
        val km = distanceMeters / 1000.0
        return when {
            km >= 100 -> "%.0f".format(km)
            km >= 10 -> "%.1f".format(km)
            else -> "%.2f".format(km)
        }
    }

    fun formatCalories(kilocalories: Double): String = kilocalories.toLong().toString()

    fun formatDurationMinutes(totalMinutes: Long): String {
        if (totalMinutes <= 0L) return "0"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun formatCount(count: Int): String = count.toString()

    fun formatAverageBpm(average: Double): String = average.roundToInt().toString()

    fun formatHeartRateVariabilityMs(millis: Double): String = millis.roundToInt().toString()

    fun formatWeightKg(kilograms: Double): String = "%.1f".format(kilograms)

    fun formatHeightCm(meters: Double): String = "%.0f".format(meters * 100)

    fun formatBloodPressure(systolic: Double, diastolic: Double): String =
        "${systolic.roundToInt()}/${diastolic.roundToInt()}"

    fun formatBloodGlucose(mmolPerLiter: Double): String = "%.1f".format(mmolPerLiter)

    fun formatPercentage(percentage: Double): String = percentage.roundToInt().toString()

    fun formatCelsius(celsius: Double): String = "%.1f".format(celsius)

    fun formatSignedCelsius(delta: Double): String =
        if (delta >= 0) "+%.1f".format(delta) else "%.1f".format(delta)

    fun formatRespiratoryRate(rate: Double): String = rate.roundToInt().toString()

    fun formatLiters(liters: Double): String = "%.1f".format(liters)

    fun formatVo2Max(mlPerMinPerKg: Double): String = "%.1f".format(mlPerMinPerKg)

    fun formatWatts(watts: Double): String = watts.roundToInt().toString()

    fun formatOptional(value: Double?, formatter: (Double) -> String): String =
        value?.let(formatter) ?: NO_DATA

    fun formatOptional(value: Long?, formatter: (Long) -> String = { it.toString() }): String =
        value?.let(formatter) ?: NO_DATA

    fun sumDurationsMinutes(durations: List<Duration>): Long =
        durations.sumOf { it.toMinutes() }
}
