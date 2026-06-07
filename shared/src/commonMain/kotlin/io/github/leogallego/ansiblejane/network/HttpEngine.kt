package io.github.leogallego.ansiblejane.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun createPlatformHttpClient(
    trustSelfSigned: Boolean = false,
    expectedFingerprint: String? = null,
    block: HttpClientConfig<*>.() -> Unit = {}
): HttpClient
