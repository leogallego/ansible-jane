package io.github.leogallego.ansiblejane.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun createPlatformHttpClient(
    trustSelfSigned: Boolean = false,
    block: HttpClientConfig<*>.() -> Unit = {}
): HttpClient
