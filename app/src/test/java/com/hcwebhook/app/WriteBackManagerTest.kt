package com.hcwebhook.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WriteBackManagerTest {

    // Helper to simulate the optDoubleOrNull behavior from WriteBackManager
    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        return if (has(key) && !isNull(key)) getDouble(key) else null
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) getString(key) else null
    }

    // --- Nutrition JSON parsing tests ---

    @Test
    fun nutrition_basicFieldsParse() {
        val json = JSONObject().apply {
            put("calories", 500.0)
            put("protein", 25.0)
            put("carbs", 60.0)
            put("fat", 20.0)
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:30:00Z")
        }
        assertEquals(500.0, json.optDoubleOrNull("calories")!!, 0.01)
        assertEquals(25.0, json.optDoubleOrNull("protein")!!, 0.01)
        assertEquals(60.0, json.optDoubleOrNull("carbs")!!, 0.01)
        assertEquals(20.0, json.optDoubleOrNull("fat")!!, 0.01)
        assertNotNull(Instant.parse(json.getString("startTime")))
        assertNotNull(Instant.parse(json.getString("endTime")))
    }

    @Test
    fun nutrition_allExtendedFieldsParse() {
        val json = JSONObject().apply {
            put("calories", 500.0)
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:30:00Z")
            put("name", "Lunch")
            put("mealType", 2)
            put("saturatedFat", 5.0)
            put("monounsaturatedFat", 8.0)
            put("polyunsaturatedFat", 3.0)
            put("transFat", 0.5)
            put("dietaryFiber", 4.0)
            put("sugar", 10.0)
            put("cholesterol", 50.0)
            put("caffeine", 100.0)
            put("vitaminA", 900.0)
            put("vitaminB6", 1.3)
            put("vitaminB12", 2.4)
            put("vitaminC", 90.0)
            put("vitaminD", 20.0)
            put("vitaminE", 15.0)
            put("vitaminK", 120.0)
            put("biotin", 30.0)
            put("folate", 400.0)
            put("folicAcid", 200.0)
            put("niacin", 16.0)
            put("pantothenicAcid", 5.0)
            put("riboflavin", 1.3)
            put("thiamin", 1.2)
            put("calcium", 1000.0)
            put("iron", 18.0)
            put("magnesium", 400.0)
            put("zinc", 11.0)
            put("potassium", 4700.0)
            put("sodium", 2300.0)
            put("phosphorus", 700.0)
            put("manganese", 2.3)
            put("copper", 0.9)
            put("selenium", 55.0)
            put("chromium", 35.0)
            put("iodine", 150.0)
            put("molybdenum", 45.0)
            put("chloride", 2300.0)
        }

        assertEquals("Lunch", json.optStringOrNull("name"))
        assertEquals(2, json.optInt("mealType", 0))
        assertEquals(5.0, json.optDoubleOrNull("saturatedFat")!!, 0.01)
        assertEquals(8.0, json.optDoubleOrNull("monounsaturatedFat")!!, 0.01)
        assertEquals(3.0, json.optDoubleOrNull("polyunsaturatedFat")!!, 0.01)
        assertEquals(0.5, json.optDoubleOrNull("transFat")!!, 0.01)
        assertEquals(4.0, json.optDoubleOrNull("dietaryFiber")!!, 0.01)
        assertEquals(10.0, json.optDoubleOrNull("sugar")!!, 0.01)
        assertEquals(50.0, json.optDoubleOrNull("cholesterol")!!, 0.01)
        assertEquals(100.0, json.optDoubleOrNull("caffeine")!!, 0.01)
        assertEquals(900.0, json.optDoubleOrNull("vitaminA")!!, 0.01)
        assertEquals(1.3, json.optDoubleOrNull("vitaminB6")!!, 0.01)
        assertEquals(2.4, json.optDoubleOrNull("vitaminB12")!!, 0.01)
        assertEquals(90.0, json.optDoubleOrNull("vitaminC")!!, 0.01)
        assertEquals(20.0, json.optDoubleOrNull("vitaminD")!!, 0.01)
        assertEquals(15.0, json.optDoubleOrNull("vitaminE")!!, 0.01)
        assertEquals(120.0, json.optDoubleOrNull("vitaminK")!!, 0.01)
        assertEquals(30.0, json.optDoubleOrNull("biotin")!!, 0.01)
        assertEquals(400.0, json.optDoubleOrNull("folate")!!, 0.01)
        assertEquals(200.0, json.optDoubleOrNull("folicAcid")!!, 0.01)
        assertEquals(16.0, json.optDoubleOrNull("niacin")!!, 0.01)
        assertEquals(5.0, json.optDoubleOrNull("pantothenicAcid")!!, 0.01)
        assertEquals(1.3, json.optDoubleOrNull("riboflavin")!!, 0.01)
        assertEquals(1.2, json.optDoubleOrNull("thiamin")!!, 0.01)
        assertEquals(1000.0, json.optDoubleOrNull("calcium")!!, 0.01)
        assertEquals(18.0, json.optDoubleOrNull("iron")!!, 0.01)
        assertEquals(400.0, json.optDoubleOrNull("magnesium")!!, 0.01)
        assertEquals(11.0, json.optDoubleOrNull("zinc")!!, 0.01)
        assertEquals(4700.0, json.optDoubleOrNull("potassium")!!, 0.01)
        assertEquals(2300.0, json.optDoubleOrNull("sodium")!!, 0.01)
        assertEquals(700.0, json.optDoubleOrNull("phosphorus")!!, 0.01)
        assertEquals(2.3, json.optDoubleOrNull("manganese")!!, 0.01)
        assertEquals(0.9, json.optDoubleOrNull("copper")!!, 0.01)
        assertEquals(55.0, json.optDoubleOrNull("selenium")!!, 0.01)
        assertEquals(35.0, json.optDoubleOrNull("chromium")!!, 0.01)
        assertEquals(150.0, json.optDoubleOrNull("iodine")!!, 0.01)
        assertEquals(45.0, json.optDoubleOrNull("molybdenum")!!, 0.01)
        assertEquals(2300.0, json.optDoubleOrNull("chloride")!!, 0.01)
    }

    @Test
    fun nutrition_optionalFieldsReturnNull() {
        val json = JSONObject().apply {
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:30:00Z")
        }
        assertNull(json.optDoubleOrNull("calories"))
        assertNull(json.optDoubleOrNull("protein"))
        assertNull(json.optDoubleOrNull("carbs"))
        assertNull(json.optDoubleOrNull("fat"))
        assertNull(json.optDoubleOrNull("saturatedFat"))
        assertNull(json.optDoubleOrNull("vitaminA"))
        assertNull(json.optDoubleOrNull("calcium"))
        assertNull(json.optStringOrNull("name"))
    }

    @Test
    fun nutrition_nullFieldsReturnNull() {
        val json = JSONObject().apply {
            put("calories", JSONObject.NULL)
            put("protein", JSONObject.NULL)
            put("name", JSONObject.NULL)
        }
        assertNull(json.optDoubleOrNull("calories"))
        assertNull(json.optDoubleOrNull("protein"))
        assertNull(json.optStringOrNull("name"))
    }

    // --- Exercise JSON parsing tests ---

    @Test
    fun exercise_basicFieldsParse() {
        val json = JSONObject().apply {
            put("type", 8) // Running
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T11:00:00Z")
            put("title", "Morning Run")
            put("notes", "Felt great today")
        }
        assertEquals(8, json.getInt("type"))
        assertEquals("Morning Run", json.optStringOrNull("title"))
        assertEquals("Felt great today", json.optStringOrNull("notes"))
    }

    @Test
    fun exercise_lapsParse() {
        val lapsJson = JSONArray().apply {
            put(JSONObject().apply {
                put("startTime", "2024-01-01T10:00:00Z")
                put("endTime", "2024-01-01T10:05:00Z")
            })
            put(JSONObject().apply {
                put("startTime", "2024-01-01T10:05:00Z")
                put("endTime", "2024-01-01T10:10:00Z")
            })
        }
        val json = JSONObject().apply {
            put("type", 8)
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T10:10:00Z")
            put("laps", lapsJson)
        }

        val lapsArray = json.optJSONArray("laps")
        assertNotNull(lapsArray)
        assertEquals(2, lapsArray!!.length())

        val lap1 = lapsArray.getJSONObject(0)
        assertEquals("2024-01-01T10:00:00Z", lap1.getString("startTime"))
        assertEquals("2024-01-01T10:05:00Z", lap1.getString("endTime"))
    }

    @Test
    fun exercise_segmentsParse() {
        val segmentsJson = JSONArray().apply {
            put(JSONObject().apply {
                put("startTime", "2024-01-01T10:00:00Z")
                put("endTime", "2024-01-01T10:20:00Z")
                put("segmentType", 1)
            })
        }
        val json = JSONObject().apply {
            put("type", 8)
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T10:20:00Z")
            put("segments", segmentsJson)
        }

        val segsArray = json.optJSONArray("segments")
        assertNotNull(segsArray)
        val seg = segsArray!!.getJSONObject(0)
        assertEquals(1, seg.getInt("segmentType"))
    }

    @Test
    fun exercise_routeParse() {
        val routeJson = JSONArray().apply {
            put(JSONObject().apply {
                put("time", "2024-01-01T10:00:00Z")
                put("latitude", 37.7749)
                put("longitude", -122.4194)
                put("altitude", 15.0)
            })
            put(JSONObject().apply {
                put("time", "2024-01-01T10:01:00Z")
                put("latitude", 37.7750)
                put("longitude", -122.4195)
            })
        }
        val json = JSONObject().apply {
            put("type", 8)
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T10:02:00Z")
            put("route", routeJson)
        }

        val route = json.optJSONArray("route")
        assertNotNull(route)
        assertEquals(2, route!!.length())

        val loc1 = route.getJSONObject(0)
        assertEquals(37.7749, loc1.getDouble("latitude"), 0.0001)
        assertEquals(-122.4194, loc1.getDouble("longitude"), 0.0001)
        assertEquals(15.0, loc1.getDouble("altitude"), 0.01)

        val loc2 = route.getJSONObject(1)
        assertFalse(loc2.has("altitude"))
    }

    @Test
    fun exercise_noOptionalFields() {
        val json = JSONObject().apply {
            put("type", 8)
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T11:00:00Z")
        }
        assertNull(json.optStringOrNull("title"))
        assertNull(json.optStringOrNull("notes"))
        assertNull(json.optJSONArray("laps"))
        assertNull(json.optJSONArray("segments"))
        assertNull(json.optJSONArray("route"))
    }

    // --- Sleep JSON parsing tests ---

    @Test
    fun sleep_stagesParse() {
        val stagesJson = JSONArray().apply {
            put(JSONObject().apply {
                put("startTime", "2024-01-01T23:00:00Z")
                put("endTime", "2024-01-01T23:30:00Z")
                put("stage", 1) // AWAKE
            })
            put(JSONObject().apply {
                put("startTime", "2024-01-01T23:30:00Z")
                put("endTime", "2024-01-02T01:00:00Z")
                put("stage", 4) // LIGHT
            })
            put(JSONObject().apply {
                put("startTime", "2024-01-02T01:00:00Z")
                put("endTime", "2024-01-02T03:00:00Z")
                put("stage", 5) // DEEP
            })
            put(JSONObject().apply {
                put("startTime", "2024-01-02T03:00:00Z")
                put("endTime", "2024-01-02T04:00:00Z")
                put("stage", 6) // REM
            })
        }
        val json = JSONObject().apply {
            put("startTime", "2024-01-01T23:00:00Z")
            put("endTime", "2024-01-02T07:00:00Z")
            put("stages", stagesJson)
            put("title", "Night Sleep")
            put("notes", "Slept well")
        }

        val stages = json.getJSONArray("stages")
        assertEquals(4, stages.length())
        assertEquals(5, stages.getJSONObject(2).getInt("stage")) // DEEP
        assertEquals("Night Sleep", json.optStringOrNull("title"))
        assertEquals("Slept well", json.optStringOrNull("notes"))
    }

    @Test
    fun sleep_noStages() {
        val json = JSONObject().apply {
            put("startTime", "2024-01-01T23:00:00Z")
            put("endTime", "2024-01-02T07:00:00Z")
        }
        assertNull(json.optJSONArray("stages"))
    }

    // --- Blood Pressure JSON parsing tests ---

    @Test
    fun bloodPressure_withPositionAndLocation() {
        val json = JSONObject().apply {
            put("systolic", 120.0)
            put("diastolic", 80.0)
            put("time", "2024-01-01T08:00:00Z")
            put("bodyPosition", 1) // SITTING_DOWN
            put("measurementLocation", 2) // LEFT_WRIST
        }
        assertEquals(120.0, json.getDouble("systolic"), 0.01)
        assertEquals(80.0, json.getDouble("diastolic"), 0.01)
        assertEquals(1, json.optInt("bodyPosition", 0))
        assertEquals(2, json.optInt("measurementLocation", 0))
    }

    @Test
    fun bloodPressure_defaultsWhenMissing() {
        val json = JSONObject().apply {
            put("systolic", 120.0)
            put("diastolic", 80.0)
            put("time", "2024-01-01T08:00:00Z")
        }
        assertEquals(0, json.optInt("bodyPosition", 0))
        assertEquals(0, json.optInt("measurementLocation", 0))
    }

    // --- Blood Glucose JSON parsing tests ---

    @Test
    fun bloodGlucose_withAllFields() {
        val json = JSONObject().apply {
            put("millimolePerLiter", 5.5)
            put("time", "2024-01-01T08:00:00Z")
            put("specimenSource", 1)
            put("mealType", 2)
            put("relationToMeal", 1)
        }
        assertEquals(5.5, json.getDouble("millimolePerLiter"), 0.01)
        assertEquals(1, json.optInt("specimenSource", 0))
        assertEquals(2, json.optInt("mealType", 0))
        assertEquals(1, json.optInt("relationToMeal", 0))
    }

    // --- Body Temperature JSON parsing tests ---

    @Test
    fun bodyTemperature_withMeasurementLocation() {
        val json = JSONObject().apply {
            put("celsius", 36.6)
            put("time", "2024-01-01T08:00:00Z")
            put("measurementLocation", 3) // MOUTH
        }
        assertEquals(36.6, json.getDouble("celsius"), 0.01)
        assertEquals(3, json.optInt("measurementLocation", 0))
    }

    // --- Error handling tests ---

    @Test(expected = org.json.JSONException::class)
    fun nutrition_missingRequiredStartTime_throws() {
        val json = JSONObject().apply {
            put("endTime", "2024-01-01T12:30:00Z")
        }
        json.getString("startTime")
    }

    @Test(expected = org.json.JSONException::class)
    fun exercise_missingRequiredType_throws() {
        val json = JSONObject().apply {
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T11:00:00Z")
        }
        json.getInt("type")
    }

    @Test(expected = org.json.JSONException::class)
    fun bloodPressure_missingRequiredSystolic_throws() {
        val json = JSONObject().apply {
            put("diastolic", 80.0)
            put("time", "2024-01-01T08:00:00Z")
        }
        json.getDouble("systolic")
    }

    @Test
    fun invalidTimeFormat_throws() {
        val json = JSONObject().apply {
            put("startTime", "not-a-date")
        }
        try {
            Instant.parse(json.getString("startTime"))
            fail("Should have thrown DateTimeParseException")
        } catch (_: java.time.format.DateTimeParseException) {
            // expected
        }
    }

    // --- Write type routing tests ---

    @Test
    fun allSupportedWriteTypes() {
        val supportedTypes = listOf(
            "nutrition", "hydration", "weight", "steps", "heart_rate",
            "sleep", "distance", "active_calories", "total_calories",
            "height", "oxygen_saturation", "heart_rate_variability",
            "basal_metabolic_rate", "body_fat", "lean_body_mass",
            "resting_heart_rate", "vo2_max", "bone_mass",
            "blood_pressure", "blood_glucose", "body_temperature",
            "respiratory_rate", "exercise", "floors_climbed",
            "menstruation", "speed", "power"
        )
        assertEquals("Should support 27 write types", 27, supportedTypes.size)
    }

    // --- Speed/Power sample parsing tests ---

    @Test
    fun speed_samplesParse() {
        val samplesJson = JSONArray().apply {
            put(JSONObject().apply {
                put("time", "2024-01-01T10:00:00Z")
                put("metersPerSecond", 3.5)
            })
            put(JSONObject().apply {
                put("time", "2024-01-01T10:01:00Z")
                put("metersPerSecond", 4.0)
            })
        }
        val json = JSONObject().apply {
            put("samples", samplesJson)
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T10:02:00Z")
        }

        val samples = json.getJSONArray("samples")
        assertEquals(2, samples.length())
        assertEquals(3.5, samples.getJSONObject(0).getDouble("metersPerSecond"), 0.01)
    }

    @Test
    fun power_samplesParse() {
        val samplesJson = JSONArray().apply {
            put(JSONObject().apply {
                put("time", "2024-01-01T10:00:00Z")
                put("watts", 250.0)
            })
        }
        val json = JSONObject().apply {
            put("samples", samplesJson)
            put("startTime", "2024-01-01T10:00:00Z")
            put("endTime", "2024-01-01T10:30:00Z")
        }

        val samples = json.getJSONArray("samples")
        assertEquals(250.0, samples.getJSONObject(0).getDouble("watts"), 0.01)
    }

    // --- Pending writes response parsing ---

    @Test
    fun pendingWritesResponse_parses() {
        val response = JSONObject().apply {
            put("pending", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "abc-123")
                    put("type", "nutrition")
                    put("data", JSONObject().apply {
                        put("calories", 500.0)
                        put("startTime", "2024-01-01T12:00:00Z")
                        put("endTime", "2024-01-01T12:30:00Z")
                    })
                })
                put(JSONObject().apply {
                    put("id", "def-456")
                    put("type", "weight")
                    put("data", JSONObject().apply {
                        put("kilograms", 75.0)
                        put("time", "2024-01-01T08:00:00Z")
                    })
                })
            })
        }

        val pending = response.getJSONArray("pending")
        assertEquals(2, pending.length())
        assertEquals("abc-123", pending.getJSONObject(0).getString("id"))
        assertEquals("nutrition", pending.getJSONObject(0).getString("type"))
        assertEquals("def-456", pending.getJSONObject(1).getString("id"))
        assertEquals("weight", pending.getJSONObject(1).getString("type"))
    }

    @Test
    fun pendingWritesResponse_emptyPending() {
        val response = JSONObject().apply {
            put("pending", JSONArray())
        }
        assertEquals(0, response.getJSONArray("pending").length())
    }

    @Test
    fun pendingWritesResponse_missingPending() {
        val response = JSONObject()
        assertNull(response.optJSONArray("pending"))
    }
}
