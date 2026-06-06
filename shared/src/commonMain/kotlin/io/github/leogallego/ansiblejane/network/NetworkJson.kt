package io.github.leogallego.ansiblejane.network

import kotlinx.serialization.json.Json

val networkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}
