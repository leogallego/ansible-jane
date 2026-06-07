package io.github.leogallego.ansiblejane.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

actual fun createPlatformHttpClient(
    trustSelfSigned: Boolean,
    expectedFingerprint: String?,
    block: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(CIO) {
    if (trustSelfSigned) {
        engine {
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        if (expectedFingerprint != null && chain != null && chain.isNotEmpty()) {
                            val serverFingerprint = sha256Fingerprint(chain[0])
                            if (!serverFingerprint.equals(expectedFingerprint, ignoreCase = true)) {
                                throw CertificateException(
                                    "Certificate fingerprint mismatch: expected $expectedFingerprint, got $serverFingerprint"
                                )
                            }
                        }
                    }
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            }
        }
    }
    block()
}

private fun sha256Fingerprint(cert: X509Certificate): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(cert.encoded).joinToString(":") { "%02X".format(it) }
}
