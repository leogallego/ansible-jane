package com.example.aapremote.network

import com.example.aapremote.data.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AapApiProvider(
    private val tokenManager: TokenManager,
    private val json: Json
) {
    private var currentBaseUrl: String? = null
    private var currentTrustSelfSigned: Boolean = false
    private var cachedApiService: AapApiService? = null

    @Synchronized
    fun getApiService(): AapApiService {
        val baseUrl = tokenManager.cachedBaseUrl
        val trustSelfSigned = tokenManager.cachedTrustSelfSigned

        if (cachedApiService == null ||
            currentBaseUrl != baseUrl ||
            currentTrustSelfSigned != trustSelfSigned
        ) {
            currentBaseUrl = baseUrl
            currentTrustSelfSigned = trustSelfSigned

            val client = buildClient(trustSelfSigned)
            val retrofit = buildRetrofit(client, baseUrl ?: "https://placeholder.example.com")
            cachedApiService = retrofit.create(AapApiService::class.java)
        }

        return cachedApiService!!
    }

    private fun buildClient(trustSelfSigned: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenManager.cachedToken })
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })

        if (trustSelfSigned) {
            val trustManager = CertTrustManager.createTrustAllManager()
            builder.sslSocketFactory(
                CertTrustManager.createSslSocketFactory(trustManager),
                trustManager
            )
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    private fun buildRetrofit(client: OkHttpClient, baseUrl: String): Retrofit {
        val apiVersion = tokenManager.cachedApiVersion
        return Retrofit.Builder()
            .baseUrl("${baseUrl.trimEnd('/')}${apiVersion.prefix}")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
