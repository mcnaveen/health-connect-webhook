package com.hcwebhook.app

/**
 * How export granularity can be configured for a [HealthDataType].
 *
 * Stored as minutes in preferences:
 * - [RESOLUTION_DAILY] (-1): one total per calendar day (interval types only)
 * - [RESOLUTION_FULL] (0): every raw record / sample (existing default for most types)
 * - [SLEEP_SUMMARY] (1): sleep sessions without stage breakdown
 * - positive N: bucket raw data into N-minute windows
 */
enum class DataResolutionFamily {
    /** Default daily aggregate; also supports full raw and N-minute buckets. */
    INTERVAL_WITH_DAILY,
    /** Default raw intervals; optional daily and N-minute buckets. */
    INTERVAL_RAW_DEFAULT,
    /** Default every sample; optional N-minute avg/min/max buckets. */
    SAMPLE_SERIES,
    /** Default full session with stages; optional summary without stages. */
    SLEEP_SESSION,
}

const val RESOLUTION_DAILY = -1
const val RESOLUTION_FULL = 0
const val SLEEP_SUMMARY = 1

val HealthDataType.resolutionFamily: DataResolutionFamily?
    get() = when (this) {
        HealthDataType.STEPS,
        HealthDataType.DISTANCE,
        HealthDataType.ACTIVE_CALORIES -> DataResolutionFamily.INTERVAL_WITH_DAILY
        HealthDataType.TOTAL_CALORIES,
        HealthDataType.HYDRATION,
        HealthDataType.NUTRITION -> DataResolutionFamily.INTERVAL_RAW_DEFAULT
        HealthDataType.HEART_RATE,
        HealthDataType.HEART_RATE_VARIABILITY,
        HealthDataType.OXYGEN_SATURATION,
        HealthDataType.RESPIRATORY_RATE,
        HealthDataType.SKIN_TEMPERATURE -> DataResolutionFamily.SAMPLE_SERIES
        HealthDataType.SLEEP -> DataResolutionFamily.SLEEP_SESSION
        else -> null
    }

/** Preserves pre-resolution behavior for each type when the user has not changed settings. */
val HealthDataType.defaultResolutionMinutes: Int
    get() = when (resolutionFamily) {
        DataResolutionFamily.INTERVAL_WITH_DAILY -> RESOLUTION_DAILY
        DataResolutionFamily.INTERVAL_RAW_DEFAULT,
        DataResolutionFamily.SAMPLE_SERIES,
        DataResolutionFamily.SLEEP_SESSION -> RESOLUTION_FULL
        null -> RESOLUTION_FULL
    }

val HealthDataType.supportsResolution: Boolean
    get() = resolutionFamily != null

fun resolutionSubtitleResId(family: DataResolutionFamily): Int = when (family) {
    DataResolutionFamily.INTERVAL_WITH_DAILY -> R.string.config_interval_daily_resolution_hint
    DataResolutionFamily.INTERVAL_RAW_DEFAULT -> R.string.config_interval_raw_resolution_hint
    DataResolutionFamily.SAMPLE_SERIES -> R.string.config_sample_resolution_hint
    DataResolutionFamily.SLEEP_SESSION -> R.string.config_sleep_resolution_hint
}
