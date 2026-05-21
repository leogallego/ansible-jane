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
) : IAapApiProvider {
    // Per-instance service cache: instanceId -> (AapApiService, EdaApiService)
    private val serviceCache = mutableMapOf<String, Pair<AapApiService, EdaApiService?>>()

    @Synchronized
    override fun getApiService(): AapApiService {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = serviceCache[instance.id]
        if (cached != null) return cached.first

        val client = buildClient(instance.token, instance.trustSelfSigned, instance.id)
        val apiVersion = try {
            ApiVersion.valueOf(instance.apiVersion)
        } catch (_: Exception) {
            ApiVersion.CONTROLLER_V2
        }
        val retrofit = buildRetrofit(client, instance.baseUrl, apiVersion)
        val apiService = retrofit.create(AapApiService::class.java)
        serviceCache[instance.id] = Pair(apiService, null)
        return apiService
    }

    @Synchronized
    override fun getEdaApiService(): EdaApiService {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = serviceCache[instance.id]
        if (cached?.second != null) return cached.second!!

        val client = buildClient(instance.token, instance.trustSelfSigned, instance.id)
        val retrofit = buildEdaRetrofit(client, instance.baseUrl)
        val edaService = retrofit.create(EdaApiService::class.java)

        val existingApi = cached?.first ?: run {
            val apiVersion = try {
                ApiVersion.valueOf(instance.apiVersion)
            } catch (_: Exception) {
                ApiVersion.CONTROLLER_V2
            }
            buildRetrofit(client, instance.baseUrl, apiVersion)
                .create(AapApiService::class.java)
        }
        serviceCache[instance.id] = Pair(existingApi, edaService)
        return edaService
    }

    @Synchronized
    override fun evictInstance(instanceId: String) {
        serviceCache.remove(instanceId)
    }

    private fun buildClient(token: String, trustSelfSigned: Boolean, instanceId: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(
                tokenProvider = { tokenManager.activeInstance.value?.token ?: token },
                instanceIdProvider = { instanceId }
            ))
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

    private fun buildRetrofit(client: OkHttpClient, baseUrl: String, apiVersion: ApiVersion): Retrofit {
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
