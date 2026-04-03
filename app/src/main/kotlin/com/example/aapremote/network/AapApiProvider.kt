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
    private var cachedEdaApiService: EdaApiService? = null

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

            val baseUrlValue = baseUrl
                ?: throw IllegalStateException("No AAP server URL configured. Please log in first.")
            val client = buildClient(trustSelfSigned)
            val retrofit = buildRetrofit(client, baseUrlValue)
            cachedApiService = retrofit.create(AapApiService::class.java)
            cachedEdaApiService = null
        }

        return cachedApiService!!
    }

    @Synchronized
    fun getEdaApiService(): EdaApiService {
        val baseUrl = tokenManager.cachedBaseUrl
        val trustSelfSigned = tokenManager.cachedTrustSelfSigned

        if (cachedEdaApiService == null ||
            currentBaseUrl != baseUrl ||
            currentTrustSelfSigned != trustSelfSigned
        ) {
            currentBaseUrl = baseUrl
            currentTrustSelfSigned = trustSelfSigned

            val baseUrlValue = baseUrl
                ?: throw IllegalStateException("No AAP server URL configured. Please log in first.")
            val client = buildClient(trustSelfSigned)
            val retrofit = buildEdaRetrofit(client, baseUrlValue)
            cachedEdaApiService = retrofit.create(EdaApiService::class.java)
        }

        return cachedEdaApiService!!
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

    private fun buildEdaRetrofit(client: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${baseUrl.trimEnd('/')}/api/eda/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
