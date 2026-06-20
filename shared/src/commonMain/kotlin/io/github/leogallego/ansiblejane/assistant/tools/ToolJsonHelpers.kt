package io.github.leogallego.ansiblejane.assistant.tools

import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

inline fun <reified T> listToolJson(key: String, count: Int, items: List<T>): String =
    buildJsonObject {
        put("count", count)
        put(key, networkJson.encodeToJsonElement(items))
    }.toString()
