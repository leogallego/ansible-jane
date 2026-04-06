package com.example.aapremote.model

import java.net.URI

data class AapInstance(
    val id: String,
    val baseUrl: String,
    val token: String,
    val alias: String? = null,
    val apiVersion: String = "v2",
    val trustSelfSigned: Boolean = false,
    val certFingerprint: String? = null
) {
    val displayLabel: String
        get() = alias ?: URI(baseUrl).host.orEmpty()

    val hostname: String
        get() = URI(baseUrl).host.orEmpty()
}
