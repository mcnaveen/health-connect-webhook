package com.hcwebhook.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Parsing/mapping tests for the three write-back record types. Mirrors the field
 * extraction WriteBackManager performs before handing values to HealthConnectManager.
 */
class WriteBackManagerTest {

    // Same helpers WriteBackManager uses for optional fields.
    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    // --- Nutrition ---

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
    fun nutrition_extendedFieldsParse() {
        val json = JSONObject().apply {
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:30:00Z")
            put("name", "Lunch")
            put("mealType", 2)
            put("sugar", 10.0)
            put("vitaminA", 900.0)
            put("calcium", 1000.0)
            put("sodium", 2300.0)
        }
        assertEquals("Lunch", json.optStringOrNull("name"))
        assertEquals(2, json.optInt("mealType", 0))
        assertEquals(10.0, json.optDoubleOrNull("sugar")!!, 0.01)
        assertEquals(900.0, json.optDoubleOrNull("vitaminA")!!, 0.01)
        assertEquals(1000.0, json.optDoubleOrNull("calcium")!!, 0.01)
        assertEquals(2300.0, json.optDoubleOrNull("sodium")!!, 0.01)
    }

    @Test
    fun nutrition_optionalAndNullFieldsReturnNull() {
        val json = JSONObject().apply {
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:30:00Z")
            put("protein", JSONObject.NULL)
            put("name", JSONObject.NULL)
        }
        assertNull(json.optDoubleOrNull("calories")) // absent
        assertNull(json.optDoubleOrNull("protein"))  // explicit null
        assertNull(json.optStringOrNull("name"))     // explicit null
        assertEquals(0, json.optInt("mealType", 0))  // default
    }

    @Test(expected = org.json.JSONException::class)
    fun nutrition_missingRequiredStartTime_throws() {
        JSONObject().apply { put("endTime", "2024-01-01T12:30:00Z") }.getString("startTime")
    }

    // --- Hydration ---

    @Test
    fun hydration_fieldsParse() {
        val json = JSONObject().apply {
            put("liters", 0.5)
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:05:00Z")
        }
        assertEquals(0.5, json.getDouble("liters"), 0.01)
        assertEquals(Instant.parse("2024-01-01T12:00:00Z"), Instant.parse(json.getString("startTime")))
        assertEquals(Instant.parse("2024-01-01T12:05:00Z"), Instant.parse(json.getString("endTime")))
    }

    @Test(expected = org.json.JSONException::class)
    fun hydration_missingRequiredLiters_throws() {
        JSONObject().apply {
            put("startTime", "2024-01-01T12:00:00Z")
            put("endTime", "2024-01-01T12:05:00Z")
        }.getDouble("liters")
    }

    // --- Weight ---

    @Test
    fun weight_fieldsParse() {
        val json = JSONObject().apply {
            put("kilograms", 75.0)
            put("time", "2024-01-01T08:00:00Z")
        }
        assertEquals(75.0, json.getDouble("kilograms"), 0.01)
        assertEquals(Instant.parse("2024-01-01T08:00:00Z"), Instant.parse(json.getString("time")))
    }

    @Test
    fun weight_invalidTimeFormat_throws() {
        val json = JSONObject().apply { put("time", "not-a-date") }
        try {
            Instant.parse(json.getString("time"))
            fail("Should have thrown DateTimeParseException")
        } catch (_: java.time.format.DateTimeParseException) {
            // expected
        }
    }
}
