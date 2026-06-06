package io.github.leogallego.ansiblejane.platform

expect class TlsTrustManager {
    fun createTrustManager(fingerprint: String?): Any
}
