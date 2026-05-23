package io.github.leogallego.ansiblejane.assistant.tools.local

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

fun buildToolSchema(
    vararg properties: Triple<String, String, String>,
    required: List<String> = emptyList()
): JsonObject = buildJsonObject {
    put("type", JsonPrimitive("object"))
    putJsonObject("properties") {
        properties.forEach { (name, type, description) ->
            putJsonObject(name) {
                put("type", JsonPrimitive(type))
                put("description", JsonPrimitive(description))
            }
        }
    }
    if (required.isNotEmpty()) {
        put("required", kotlinx.serialization.json.buildJsonArray {
            required.forEach { add(JsonPrimitive(it)) }
        })
    }
}
