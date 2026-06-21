package com.hcwebhook.app

import android.content.Context
import org.json.JSONObject
import java.time.Instant

/**
 * Writes a record received over the local HTTP server straight into Health Connect.
 * Direct receive only — no polling. Supported types: nutrition, hydration, weight.
 */
class WriteBackManager(context: Context) {

    private val healthConnectManager = HealthConnectManager(context)

    suspend fun write(type: String, data: JSONObject): Result<Unit> {
        return when (type) {
            "nutrition" -> writeNutrition(data)
            "hydration" -> writeHydration(data)
            "weight" -> writeWeight(data)
            else -> Result.failure(IllegalArgumentException("Unsupported write type: $type"))
        }
    }

    private suspend fun writeNutrition(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertNutrition(
            calories = data.optDoubleOrNull("calories"),
            protein = data.optDoubleOrNull("protein"),
            carbs = data.optDoubleOrNull("carbs"),
            fat = data.optDoubleOrNull("fat"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime")),
            name = data.optStringOrNull("name"),
            mealType = data.optInt("mealType", 0),
            saturatedFat = data.optDoubleOrNull("saturatedFat"),
            monounsaturatedFat = data.optDoubleOrNull("monounsaturatedFat"),
            polyunsaturatedFat = data.optDoubleOrNull("polyunsaturatedFat"),
            transFat = data.optDoubleOrNull("transFat"),
            dietaryFiber = data.optDoubleOrNull("dietaryFiber"),
            sugar = data.optDoubleOrNull("sugar"),
            cholesterol = data.optDoubleOrNull("cholesterol"),
            caffeine = data.optDoubleOrNull("caffeine"),
            vitaminA = data.optDoubleOrNull("vitaminA"),
            vitaminB6 = data.optDoubleOrNull("vitaminB6"),
            vitaminB12 = data.optDoubleOrNull("vitaminB12"),
            vitaminC = data.optDoubleOrNull("vitaminC"),
            vitaminD = data.optDoubleOrNull("vitaminD"),
            vitaminE = data.optDoubleOrNull("vitaminE"),
            vitaminK = data.optDoubleOrNull("vitaminK"),
            biotin = data.optDoubleOrNull("biotin"),
            folate = data.optDoubleOrNull("folate"),
            folicAcid = data.optDoubleOrNull("folicAcid"),
            niacin = data.optDoubleOrNull("niacin"),
            pantothenicAcid = data.optDoubleOrNull("pantothenicAcid"),
            riboflavin = data.optDoubleOrNull("riboflavin"),
            thiamin = data.optDoubleOrNull("thiamin"),
            calcium = data.optDoubleOrNull("calcium"),
            iron = data.optDoubleOrNull("iron"),
            magnesium = data.optDoubleOrNull("magnesium"),
            zinc = data.optDoubleOrNull("zinc"),
            potassium = data.optDoubleOrNull("potassium"),
            sodium = data.optDoubleOrNull("sodium"),
            phosphorus = data.optDoubleOrNull("phosphorus"),
            manganese = data.optDoubleOrNull("manganese"),
            copper = data.optDoubleOrNull("copper"),
            selenium = data.optDoubleOrNull("selenium"),
            chromium = data.optDoubleOrNull("chromium"),
            iodine = data.optDoubleOrNull("iodine"),
            molybdenum = data.optDoubleOrNull("molybdenum"),
            chloride = data.optDoubleOrNull("chloride")
        )
    }

    private suspend fun writeHydration(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertHydration(
            liters = data.getDouble("liters"),
            startTime = Instant.parse(data.getString("startTime")),
            endTime = Instant.parse(data.getString("endTime"))
        )
    }

    private suspend fun writeWeight(data: JSONObject): Result<Unit> {
        return healthConnectManager.insertWeight(
            kilograms = data.getDouble("kilograms"),
            time = Instant.parse(data.getString("time"))
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
