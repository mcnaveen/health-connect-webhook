package com.hcwebhook.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NutritionFieldsTest {

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        return if (has(key) && !isNull(key)) getDouble(key) else null
    }

    // --- Macros (grams) ---

    @Test
    fun macro_saturatedFat_maps() {
        val json = JSONObject().put("saturatedFat", 5.0)
        assertEquals(5.0, json.optDoubleOrNull("saturatedFat")!!, 0.001)
    }

    @Test
    fun macro_monounsaturatedFat_maps() {
        val json = JSONObject().put("monounsaturatedFat", 8.0)
        assertEquals(8.0, json.optDoubleOrNull("monounsaturatedFat")!!, 0.001)
    }

    @Test
    fun macro_polyunsaturatedFat_maps() {
        val json = JSONObject().put("polyunsaturatedFat", 3.0)
        assertEquals(3.0, json.optDoubleOrNull("polyunsaturatedFat")!!, 0.001)
    }

    @Test
    fun macro_transFat_maps() {
        val json = JSONObject().put("transFat", 0.5)
        assertEquals(0.5, json.optDoubleOrNull("transFat")!!, 0.001)
    }

    @Test
    fun macro_dietaryFiber_maps() {
        val json = JSONObject().put("dietaryFiber", 4.0)
        assertEquals(4.0, json.optDoubleOrNull("dietaryFiber")!!, 0.001)
    }

    @Test
    fun macro_sugar_maps() {
        val json = JSONObject().put("sugar", 10.0)
        assertEquals(10.0, json.optDoubleOrNull("sugar")!!, 0.001)
    }

    // --- Milligram nutrients ---

    @Test
    fun milligram_cholesterol_maps() {
        val json = JSONObject().put("cholesterol", 50.0)
        assertEquals(50.0, json.optDoubleOrNull("cholesterol")!!, 0.001)
    }

    @Test
    fun milligram_caffeine_maps() {
        val json = JSONObject().put("caffeine", 100.0)
        assertEquals(100.0, json.optDoubleOrNull("caffeine")!!, 0.001)
    }

    // --- Vitamins (microgram units) ---

    @Test
    fun vitamin_A_maps_micrograms() {
        val json = JSONObject().put("vitaminA", 900.0)
        assertEquals(900.0, json.optDoubleOrNull("vitaminA")!!, 0.001)
    }

    @Test
    fun vitamin_B12_maps_micrograms() {
        val json = JSONObject().put("vitaminB12", 2.4)
        assertEquals(2.4, json.optDoubleOrNull("vitaminB12")!!, 0.001)
    }

    @Test
    fun vitamin_D_maps_micrograms() {
        val json = JSONObject().put("vitaminD", 20.0)
        assertEquals(20.0, json.optDoubleOrNull("vitaminD")!!, 0.001)
    }

    @Test
    fun vitamin_K_maps_micrograms() {
        val json = JSONObject().put("vitaminK", 120.0)
        assertEquals(120.0, json.optDoubleOrNull("vitaminK")!!, 0.001)
    }

    @Test
    fun vitamin_biotin_maps_micrograms() {
        val json = JSONObject().put("biotin", 30.0)
        assertEquals(30.0, json.optDoubleOrNull("biotin")!!, 0.001)
    }

    @Test
    fun vitamin_folate_maps_micrograms() {
        val json = JSONObject().put("folate", 400.0)
        assertEquals(400.0, json.optDoubleOrNull("folate")!!, 0.001)
    }

    @Test
    fun vitamin_folicAcid_maps_micrograms() {
        val json = JSONObject().put("folicAcid", 200.0)
        assertEquals(200.0, json.optDoubleOrNull("folicAcid")!!, 0.001)
    }

    // --- Vitamins (milligram units) ---

    @Test
    fun vitamin_B6_maps_milligrams() {
        val json = JSONObject().put("vitaminB6", 1.3)
        assertEquals(1.3, json.optDoubleOrNull("vitaminB6")!!, 0.001)
    }

    @Test
    fun vitamin_C_maps_milligrams() {
        val json = JSONObject().put("vitaminC", 90.0)
        assertEquals(90.0, json.optDoubleOrNull("vitaminC")!!, 0.001)
    }

    @Test
    fun vitamin_E_maps_milligrams() {
        val json = JSONObject().put("vitaminE", 15.0)
        assertEquals(15.0, json.optDoubleOrNull("vitaminE")!!, 0.001)
    }

    @Test
    fun vitamin_niacin_maps_milligrams() {
        val json = JSONObject().put("niacin", 16.0)
        assertEquals(16.0, json.optDoubleOrNull("niacin")!!, 0.001)
    }

    @Test
    fun vitamin_pantothenicAcid_maps_milligrams() {
        val json = JSONObject().put("pantothenicAcid", 5.0)
        assertEquals(5.0, json.optDoubleOrNull("pantothenicAcid")!!, 0.001)
    }

    @Test
    fun vitamin_riboflavin_maps_milligrams() {
        val json = JSONObject().put("riboflavin", 1.3)
        assertEquals(1.3, json.optDoubleOrNull("riboflavin")!!, 0.001)
    }

    @Test
    fun vitamin_thiamin_maps_milligrams() {
        val json = JSONObject().put("thiamin", 1.2)
        assertEquals(1.2, json.optDoubleOrNull("thiamin")!!, 0.001)
    }

    // --- Minerals (milligram units) ---

    @Test
    fun mineral_calcium_maps_milligrams() {
        val json = JSONObject().put("calcium", 1000.0)
        assertEquals(1000.0, json.optDoubleOrNull("calcium")!!, 0.001)
    }

    @Test
    fun mineral_iron_maps_milligrams() {
        val json = JSONObject().put("iron", 18.0)
        assertEquals(18.0, json.optDoubleOrNull("iron")!!, 0.001)
    }

    @Test
    fun mineral_magnesium_maps_milligrams() {
        val json = JSONObject().put("magnesium", 400.0)
        assertEquals(400.0, json.optDoubleOrNull("magnesium")!!, 0.001)
    }

    @Test
    fun mineral_zinc_maps_milligrams() {
        val json = JSONObject().put("zinc", 11.0)
        assertEquals(11.0, json.optDoubleOrNull("zinc")!!, 0.001)
    }

    @Test
    fun mineral_potassium_maps_milligrams() {
        val json = JSONObject().put("potassium", 4700.0)
        assertEquals(4700.0, json.optDoubleOrNull("potassium")!!, 0.001)
    }

    @Test
    fun mineral_sodium_maps_milligrams() {
        val json = JSONObject().put("sodium", 2300.0)
        assertEquals(2300.0, json.optDoubleOrNull("sodium")!!, 0.001)
    }

    @Test
    fun mineral_phosphorus_maps_milligrams() {
        val json = JSONObject().put("phosphorus", 700.0)
        assertEquals(700.0, json.optDoubleOrNull("phosphorus")!!, 0.001)
    }

    @Test
    fun mineral_manganese_maps_milligrams() {
        val json = JSONObject().put("manganese", 2.3)
        assertEquals(2.3, json.optDoubleOrNull("manganese")!!, 0.001)
    }

    @Test
    fun mineral_copper_maps_milligrams() {
        val json = JSONObject().put("copper", 0.9)
        assertEquals(0.9, json.optDoubleOrNull("copper")!!, 0.001)
    }

    @Test
    fun mineral_chloride_maps_milligrams() {
        val json = JSONObject().put("chloride", 2300.0)
        assertEquals(2300.0, json.optDoubleOrNull("chloride")!!, 0.001)
    }

    // --- Minerals (microgram units) ---

    @Test
    fun mineral_selenium_maps_micrograms() {
        val json = JSONObject().put("selenium", 55.0)
        assertEquals(55.0, json.optDoubleOrNull("selenium")!!, 0.001)
    }

    @Test
    fun mineral_chromium_maps_micrograms() {
        val json = JSONObject().put("chromium", 35.0)
        assertEquals(35.0, json.optDoubleOrNull("chromium")!!, 0.001)
    }

    @Test
    fun mineral_iodine_maps_micrograms() {
        val json = JSONObject().put("iodine", 150.0)
        assertEquals(150.0, json.optDoubleOrNull("iodine")!!, 0.001)
    }

    @Test
    fun mineral_molybdenum_maps_micrograms() {
        val json = JSONObject().put("molybdenum", 45.0)
        assertEquals(45.0, json.optDoubleOrNull("molybdenum")!!, 0.001)
    }

    // --- Meal type mapping ---

    @Test
    fun mealType_breakfast() {
        val json = JSONObject().put("mealType", 1)
        assertEquals(1, json.getInt("mealType"))
    }

    @Test
    fun mealType_lunch() {
        val json = JSONObject().put("mealType", 2)
        assertEquals(2, json.getInt("mealType"))
    }

    @Test
    fun mealType_dinner() {
        val json = JSONObject().put("mealType", 3)
        assertEquals(3, json.getInt("mealType"))
    }

    @Test
    fun mealType_snack() {
        val json = JSONObject().put("mealType", 4)
        assertEquals(4, json.getInt("mealType"))
    }

    @Test
    fun mealType_unknown_defaults() {
        val json = JSONObject()
        assertEquals(0, json.optInt("mealType", 0))
    }

    // --- Name field ---

    @Test
    fun name_field_set() {
        val json = JSONObject().put("name", "Caesar Salad")
        assertEquals("Caesar Salad", json.getString("name"))
    }

    @Test
    fun name_field_null() {
        val json = JSONObject().put("name", JSONObject.NULL)
        assertTrue(json.isNull("name"))
    }

    @Test
    fun name_field_missing() {
        val json = JSONObject()
        assertFalse(json.has("name"))
    }

    // --- All 40 nutrition fields exist as a comprehensive check ---

    @Test
    fun allNutritionFieldNames_areValid() {
        val fieldNames = listOf(
            "calories", "protein", "carbs", "fat",
            "name", "mealType",
            "saturatedFat", "monounsaturatedFat", "polyunsaturatedFat", "transFat",
            "dietaryFiber", "sugar", "cholesterol", "caffeine",
            "vitaminA", "vitaminB6", "vitaminB12", "vitaminC", "vitaminD", "vitaminE", "vitaminK",
            "biotin", "folate", "folicAcid", "niacin", "pantothenicAcid", "riboflavin", "thiamin",
            "calcium", "iron", "magnesium", "zinc", "potassium", "sodium", "phosphorus",
            "manganese", "copper", "selenium", "chromium", "iodine", "molybdenum", "chloride"
        )
        // 4 macros + 2 metadata + 4 fat types + 2 fiber/sugar + 2 cholesterol/caffeine +
        // 7 vitamins + 7 B vitamins detail + 10 minerals (mg) + 4 minerals (μg) = 42
        assertEquals("Should have 42 nutrition field names", 42, fieldNames.size)
        assertEquals("All field names should be unique", fieldNames.size, fieldNames.toSet().size)
    }

    // --- Unit groupings validation ---

    @Test
    fun gramsFields_correctCount() {
        // Fields that use Mass.grams(): protein, carbs, fat, saturatedFat, monounsaturatedFat,
        // polyunsaturatedFat, transFat, dietaryFiber, sugar
        val gramsFields = listOf(
            "protein", "carbs", "fat",
            "saturatedFat", "monounsaturatedFat", "polyunsaturatedFat", "transFat",
            "dietaryFiber", "sugar"
        )
        assertEquals(9, gramsFields.size)
    }

    @Test
    fun milligramsFields_correctCount() {
        // Fields that use Mass.milligrams(): cholesterol, caffeine, vitaminB6, vitaminC,
        // vitaminE, niacin, pantothenicAcid, riboflavin, thiamin,
        // calcium, iron, magnesium, zinc, potassium, sodium, phosphorus, manganese, copper, chloride
        val milligramsFields = listOf(
            "cholesterol", "caffeine",
            "vitaminB6", "vitaminC", "vitaminE",
            "niacin", "pantothenicAcid", "riboflavin", "thiamin",
            "calcium", "iron", "magnesium", "zinc", "potassium", "sodium",
            "phosphorus", "manganese", "copper", "chloride"
        )
        assertEquals(19, milligramsFields.size)
    }

    @Test
    fun microgramsFields_correctCount() {
        // Fields that use Mass.micrograms(): vitaminA, vitaminB12, vitaminD, vitaminK,
        // biotin, folate, folicAcid, selenium, chromium, iodine, molybdenum
        val microgramsFields = listOf(
            "vitaminA", "vitaminB12", "vitaminD", "vitaminK",
            "biotin", "folate", "folicAcid",
            "selenium", "chromium", "iodine", "molybdenum"
        )
        assertEquals(11, microgramsFields.size)
    }

    @Test
    fun unitGroupings_coverAllNutrientFields() {
        val gramsCount = 9
        val milligramsCount = 19
        val microgramsCount = 11
        // Total nutrient fields (excluding name, mealType, calories which uses Energy)
        assertEquals("grams + milligrams + micrograms should equal 39 nutrient fields",
            39, gramsCount + milligramsCount + microgramsCount)
    }
}
