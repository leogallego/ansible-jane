package io.github.leogallego.ansiblejane.platform

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

actual class TlsTrustManager {
    actual fun createTrustManager(fingerprint: String?): Any {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }
}
