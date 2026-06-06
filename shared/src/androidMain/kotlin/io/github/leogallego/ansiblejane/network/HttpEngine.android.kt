package io.github.leogallego.ansiblejane.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual fun createPlatformHttpClient(
    trustSelfSigned: Boolean,
    block: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(OkHttp) {
    if (trustSelfSigned) {
        engine {
            config {
                val tm = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
                val sc = SSLContext.getInstance("TLSv1.3")
                sc.init(null, arrayOf<TrustManager>(tm), null)
                sslSocketFactory(sc.socketFactory, tm)
                hostnameVerifier { _, _ -> true }
            }
        }
    }
    block()
}
