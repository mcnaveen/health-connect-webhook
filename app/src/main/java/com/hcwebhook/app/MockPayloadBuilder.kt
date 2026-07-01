package com.hcwebhook.app

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Generates a realistic mock health payload that mirrors the exact JSON structure
 * produced by SyncManager.buildJsonPayload. Add new data types here when SyncManager
 * is updated with new health types.
 *
 * Pass [enabledTypes] to include only the types the user has enabled globally.
 * Null includes all types (useful for testing without a context).
 */
object MockPayloadBuilder {
    fun build(enabledTypes: Set<String>? = null, appVersion: String = "unknown"): String {
        val now = Instant.now()
        val dayStart = now.truncatedTo(ChronoUnit.DAYS)
        val yesterday = dayStart.minus(1, ChronoUnit.DAYS)
        val t = { h: Long, m: Long -> yesterday.plus(h * 60 + m, ChronoUnit.MINUTES) }
        fun include(type: String) = enabledTypes == null || type in enabledTypes

        return buildJsonObject {
            put("timestamp", now.toString())
            put("app_version", appVersion)
            put("test", true)

            if (include(HealthDataType.STEPS.name)) {
                putJsonArray("steps") {
                    add(buildJsonObject {
                        put("count", 8432L)
                        put("start_time", yesterday.toString())
                        put("end_time", t(10, 30).toString())
                        putJsonObject("metadata") {
                            put("data_origin", "com.google.android.apps.fitness")
                            put("recording_method", "automatically_recorded")
                        }
                    })
                }
            }
            if (include(HealthDataType.SLEEP.name)) {
                putJsonArray("sleep") {
                    add(buildJsonObject {
                        put("session_end_time", t(7, 0).toString())
                        put("duration_seconds", 27000L)
                        putJsonArray("stages") {
                            add(buildJsonObject {
                                put("stage", "deep")
                                put("start_time", yesterday.toString())
                                put("end_time", t(2, 0).toString())
                                put("duration_seconds", 7200L)
                            })
                            add(buildJsonObject {
                                put("stage", "rem")
                                put("start_time", t(2, 0).toString())
                                put("end_time", t(5, 30).toString())
                                put("duration_seconds", 12600L)
                            })
                            add(buildJsonObject {
                                put("stage", "light")
                                put("start_time", t(5, 30).toString())
                                put("end_time", t(7, 0).toString())
                                put("duration_seconds", 5400L)
                            })
                        }
                    })
                }
            }
            if (include(HealthDataType.HEART_RATE.name)) {
                putJsonArray("heart_rate") {
                    add(buildJsonObject { put("bpm", 72L); put("time", t(9, 0).toString()) })
                }
            }
            if (include(HealthDataType.HEART_RATE_VARIABILITY.name)) {
                putJsonArray("heart_rate_variability") {
                    add(buildJsonObject { put("rmssd_millis", 42.5); put("time", t(6, 30).toString()) })
                }
            }
            if (include(HealthDataType.DISTANCE.name)) {
                putJsonArray("distance") {
                    add(buildJsonObject {
                        put("meters", 5420.0)
                        put("start_time", t(8, 0).toString())
                        put("end_time", t(9, 0).toString())
                        putJsonObject("metadata") {
                            put("data_origin", "com.google.android.apps.fitness")
                            put("recording_method", "automatically_recorded")
                        }
                    })
                }
            }
            if (include(HealthDataType.ACTIVE_CALORIES.name)) {
                putJsonArray("active_calories") {
                    add(buildJsonObject {
                        put("calories", 312.0)
                        put("start_time", t(8, 0).toString())
                        put("end_time", t(9, 0).toString())
                        putJsonObject("metadata") {
                            put("data_origin", "com.google.android.apps.fitness")
                            put("recording_method", "automatically_recorded")
                        }
                    })
                }
            }
            if (include(HealthDataType.TOTAL_CALORIES.name)) {
                putJsonArray("total_calories") {
                    add(buildJsonObject {
                        put("calories", 2100.0)
                        put("start_time", yesterday.toString())
                        put("end_time", t(10, 30).toString())
                    })
                }
            }
            if (include(HealthDataType.WEIGHT.name)) {
                putJsonArray("weight") {
                    add(buildJsonObject { put("kilograms", 75.5); put("time", t(7, 30).toString()) })
                }
            }
            if (include(HealthDataType.HEIGHT.name)) {
                putJsonArray("height") {
                    add(buildJsonObject { put("meters", 1.78); put("time", t(7, 30).toString()) })
                }
            }
            if (include(HealthDataType.BLOOD_PRESSURE.name)) {
                putJsonArray("blood_pressure") {
                    add(buildJsonObject {
                        put("systolic", 120.0)
                        put("diastolic", 80.0)
                        put("time", t(8, 30).toString())
                    })
                }
            }
            if (include(HealthDataType.BLOOD_GLUCOSE.name)) {
                putJsonArray("blood_glucose") {
                    add(buildJsonObject { put("mmol_per_liter", 5.4); put("time", t(8, 0).toString()) })
                }
            }
            if (include(HealthDataType.OXYGEN_SATURATION.name)) {
                putJsonArray("oxygen_saturation") {
                    add(buildJsonObject { put("percentage", 98.5); put("time", t(6, 30).toString()) })
                }
            }
            if (include(HealthDataType.BODY_TEMPERATURE.name)) {
                putJsonArray("body_temperature") {
                    add(buildJsonObject { put("celsius", 36.6); put("time", t(7, 0).toString()) })
                }
            }
            if (include(HealthDataType.SKIN_TEMPERATURE.name)) {
                putJsonArray("skin_temperature") {
                    add(buildJsonObject {
                        put("time", t(7, 0).toString())
                        put("delta_celsius", 0.1)
                        put("baseline_celsius", 36.5)
                        put("measurement_location", "wrist")
                    })
                }
            }
            if (include(HealthDataType.RESPIRATORY_RATE.name)) {
                putJsonArray("respiratory_rate") {
                    add(buildJsonObject { put("rate", 14.0); put("time", t(6, 30).toString()) })
                }
            }
            if (include(HealthDataType.RESTING_HEART_RATE.name)) {
                putJsonArray("resting_heart_rate") {
                    add(buildJsonObject { put("bpm", 58L); put("time", t(6, 0).toString()) })
                }
            }
            if (include(HealthDataType.EXERCISE.name)) {
                putJsonArray("exercise") {
                    add(buildJsonObject {
                        put("type", "running")
                        put("start_time", t(8, 0).toString())
                        put("end_time", t(9, 0).toString())
                        put("duration_seconds", 3600L)
                        put("distance_meters", 5420.0)
                        put("steps", 6800L)
                        put("avg_cadence_spm", 170.0)
                        put("max_cadence_spm", 182.0)
                        put("stride_length_m", 1.2)
                    })
                }
            }
            if (include(HealthDataType.HYDRATION.name)) {
                putJsonArray("hydration") {
                    add(buildJsonObject {
                        put("liters", 0.5)
                        put("start_time", t(8, 30).toString())
                        put("end_time", t(8, 31).toString())
                    })
                }
            }
            if (include(HealthDataType.NUTRITION.name)) {
                putJsonArray("nutrition") {
                    add(buildJsonObject {
                        put("calories", 350.0)
                        put("protein_grams", 25.0)
                        put("carbs_grams", 45.0)
                        put("fat_grams", 10.0)
                        put("sugar_grams", 8.0)
                        put("sodium_grams", 0.8)
                        put("dietary_fiber_grams", 5.0)
                        put("name", "Oatmeal")
                        put("start_time", t(7, 30).toString())
                        put("end_time", t(7, 31).toString())
                    })
                }
            }
            if (include(HealthDataType.BASAL_METABOLIC_RATE.name)) {
                putJsonArray("basal_metabolic_rate") {
                    add(buildJsonObject { put("watts", 85.0); put("time", t(6, 0).toString()) })
                }
            }
            if (include(HealthDataType.BODY_FAT.name)) {
                putJsonArray("body_fat") {
                    add(buildJsonObject { put("percentage", 18.5); put("time", t(7, 30).toString()) })
                }
            }
            if (include(HealthDataType.LEAN_BODY_MASS.name)) {
                putJsonArray("lean_body_mass") {
                    add(buildJsonObject { put("kilograms", 61.5); put("time", t(7, 30).toString()) })
                }
            }
            if (include(HealthDataType.VO2_MAX.name)) {
                putJsonArray("vo2_max") {
                    add(buildJsonObject { put("ml_per_kg_per_min", 48.0); put("time", t(9, 0).toString()) })
                }
            }
            if (include(HealthDataType.BONE_MASS.name)) {
                putJsonArray("bone_mass") {
                    add(buildJsonObject { put("kilograms", 3.2); put("time", t(7, 30).toString()) })
                }
            }
            if (include(HealthDataType.MENSTRUATION_FLOW.name)) {
                putJsonArray("menstruation_flow") {
                    add(buildJsonObject { put("flow", 2); put("time", t(8, 0).toString()) })
                }
            }
            if (include(HealthDataType.MENSTRUATION_PERIOD.name)) {
                putJsonArray("menstruation_period") {
                    add(buildJsonObject {
                        put("start_time", yesterday.toString())
                        put("end_time", t(23, 59).toString())
                    })
                }
            }
            if (include(HealthDataType.INTERMENSTRUAL_BLEEDING.name)) {
                putJsonArray("intermenstrual_bleeding") {
                    add(buildJsonObject { put("time", t(14, 0).toString()) })
                }
            }
            if (include(HealthDataType.OVULATION_TEST.name)) {
                putJsonArray("ovulation_test") {
                    add(buildJsonObject { put("result", 1); put("time", t(9, 0).toString()) })
                }
            }
            if (include(HealthDataType.CERVICAL_MUCUS.name)) {
                putJsonArray("cervical_mucus") {
                    add(buildJsonObject { put("appearance", 3); put("time", t(8, 30).toString()) })
                }
            }
            if (include(HealthDataType.SEXUAL_ACTIVITY.name)) {
                putJsonArray("sexual_activity") {
                    add(buildJsonObject { put("protection_used", 1); put("time", t(22, 0).toString()) })
                }
            }
            if (include(HealthDataType.BASAL_BODY_TEMPERATURE.name)) {
                putJsonArray("basal_body_temperature") {
                    add(buildJsonObject {
                        put("celsius", 36.4)
                        put("measurement_location", 1)
                        put("time", t(6, 30).toString())
                    })
                }
            }
        }.toString()
    }
}
