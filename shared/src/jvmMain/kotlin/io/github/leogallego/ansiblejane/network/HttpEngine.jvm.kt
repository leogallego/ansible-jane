package io.github.leogallego.ansiblejane.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

actual fun createPlatformHttpClient(
    trustSelfSigned: Boolean,
    block: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(CIO) {
    if (trustSelfSigned) {
        engine {
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            }
        }
    }
    block()
}
