package com.hcwebhook.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object WebhookPayloadTransformer {
    private val json = Json { encodeDefaults = true }

    private val ROOT_METADATA_KEYS = setOf("timestamp", "app_version", "test")

    fun transform(payload: String, preset: WebhookPayloadPreset): String {
        if (preset == WebhookPayloadPreset.DEFAULT) return payload
        val root = json.parseToJsonElement(payload).jsonObject
        val transformed = when (preset) {
            WebhookPayloadPreset.CAMEL_CASE -> JsonObject(transformKeys(root, ::snakeToCamel))
            WebhookPayloadPreset.OPEN_WEARABLES -> toOpenWearables(root)
            WebhookPayloadPreset.DEFAULT -> root
        }
        return json.encodeToString(JsonElement.serializer(), transformed)
    }

    internal fun snakeToCamel(key: String): String =
        key.split('_').mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")

    private fun transformKeys(
        obj: JsonObject,
        keyTransform: (String) -> String
    ): Map<String, JsonElement> =
        obj.map { (key, value) ->
            keyTransform(key) to transformElement(value, keyTransform)
        }.toMap()

    private fun transformElement(
        element: JsonElement,
        keyTransform: (String) -> String
    ): JsonElement =
        when (element) {
            is JsonObject -> JsonObject(transformKeys(element, keyTransform))
            is JsonArray -> JsonArray(element.map { transformElement(it, keyTransform) })
            else -> element
        }

    private fun toOpenWearables(root: JsonObject): JsonObject {
        val timestamp = root["timestamp"]?.jsonPrimitive?.contentOrNull().orEmpty()
        val appVersion = root["app_version"]?.jsonPrimitive?.contentOrNull().orEmpty()

        val records = buildList {
            for ((key, value) in root) {
                if (key in ROOT_METADATA_KEYS) continue
                val array = value as? JsonArray ?: continue
                for (item in array) {
                    val record = item as? JsonObject ?: continue
                    add(buildJsonObject {
                        put("type", key)
                        val camelRecord = transformElement(record, ::snakeToCamel) as JsonObject
                        camelRecord.forEach { (field, fieldValue) -> put(field, fieldValue) }
                    })
                }
            }
        }

        return buildJsonObject {
            put("type", "health_data.synced")
            put("timestamp", timestamp)
            putJsonObject("data") {
                put("appVersion", appVersion)
                putJsonObject("source") {
                    put("provider", "hc_webhook")
                }
                putJsonArray("records") {
                    records.forEach { add(it) }
                }
            }
        }
    }

    private fun JsonPrimitive.contentOrNull(): String? = if (isString) content else null
}
