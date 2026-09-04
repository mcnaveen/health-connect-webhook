package com.hcwebhook.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebhookPayloadTransformerTest {
    private val samplePayload = """
        {
          "timestamp": "2026-05-09T12:34:56.789Z",
          "app_version": "1.9.17",
          "steps": [
            {
              "count": 8432,
              "start_time": "2026-05-08T00:00:00Z",
              "end_time": "2026-05-08T10:30:00Z"
            }
          ],
          "heart_rate": [
            {
              "bpm": 72,
              "time": "2026-05-08T09:00:00Z"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun defaultPreset_returnsOriginalPayload() {
        val result = WebhookPayloadTransformer.transform(samplePayload, WebhookPayloadPreset.DEFAULT)
        assertEquals(samplePayload, result)
    }

    @Test
    fun camelCasePreset_renamesRootAndNestedKeys() {
        val result = WebhookPayloadTransformer.transform(samplePayload, WebhookPayloadPreset.CAMEL_CASE)
        val root = Json.parseToJsonElement(result).jsonObject

        assertTrue(root.containsKey("appVersion"))
        assertTrue(root.containsKey("heartRate"))
        assertEquals(
            "2026-05-08T00:00:00Z",
            root["steps"]!!.jsonArray.first().jsonObject["startTime"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun openWearablesPreset_wrapsRecordsUnderData() {
        val result = WebhookPayloadTransformer.transform(samplePayload, WebhookPayloadPreset.OPEN_WEARABLES)
        val root = Json.parseToJsonElement(result).jsonObject

        assertEquals("health_data.synced", root["type"]!!.jsonPrimitive.content)
        val data = root["data"]!!.jsonObject
        assertEquals("1.9.17", data["appVersion"]!!.jsonPrimitive.content)
        assertEquals("hc_webhook", data["source"]!!.jsonObject["provider"]!!.jsonPrimitive.content)

        val records = data["records"]!!.jsonArray
        assertEquals(2, records.size)

        val stepRecord = records.first().jsonObject
        assertEquals("steps", stepRecord["type"]!!.jsonPrimitive.content)
        assertEquals(8432, stepRecord["count"]!!.jsonPrimitive.content.toInt())
        assertTrue(stepRecord.containsKey("startTime"))

        val heartRateRecord = records[1].jsonObject
        assertEquals("heart_rate", heartRateRecord["type"]!!.jsonPrimitive.content)
        assertEquals(72, heartRateRecord["bpm"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun snakeToCamel_handlesMultiWordKeys() {
        assertEquals("heartRateVariability", WebhookPayloadTransformer.snakeToCamel("heart_rate_variability"))
        assertEquals("mmolPerLiter", WebhookPayloadTransformer.snakeToCamel("mmol_per_liter"))
    }
}
