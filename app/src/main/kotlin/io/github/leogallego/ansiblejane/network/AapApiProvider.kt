package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.data.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import io.github.leogallego.ansiblejane.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private data class ServiceCache(
    val controller: AapApiService,
    val eda: EdaApiService? = null,
    val platform: PlatformApiService? = null
)

class AapApiProvider(
    private val tokenManager: TokenManager,
    private val json: Json
) : IAapApiProvider {
    private val serviceCache = mutableMapOf<String, ServiceCache>()

    @Synchronized
    override fun getApiService(): AapApiService {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = serviceCache[instance.id]
        if (cached != null) return cached.controller

        val client = buildClient(instance.token, instance.trustSelfSigned, instance.id)
        val apiVersion = resolveApiVersion(instance.apiVersion)
        val retrofit = buildRetrofit(client, instance.baseUrl, apiVersion)
        val apiService = retrofit.create(AapApiService::class.java)
        serviceCache[instance.id] = ServiceCache(controller = apiService)
        return apiService
    }

    @Synchronized
    override fun getEdaApiService(): EdaApiService {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = serviceCache[instance.id]
        if (cached?.eda != null) return cached.eda!!

        val client = buildClient(instance.token, instance.trustSelfSigned, instance.id)
        val retrofit = buildComponentRetrofit(client, instance.baseUrl, "/api/eda/v1/")
        val edaService = retrofit.create(EdaApiService::class.java)

        val controllerService = cached?.controller ?: run {
            val apiVersion = resolveApiVersion(instance.apiVersion)
            buildRetrofit(client, instance.baseUrl, apiVersion)
                .create(AapApiService::class.java)
        }
        serviceCache[instance.id] = (cached ?: ServiceCache(controller = controllerService))
            .copy(eda = edaService)
        return edaService
    }

    @Synchronized
    override fun getPlatformApiService(): PlatformApiService {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = serviceCache[instance.id]
        if (cached?.platform != null) return cached.platform!!

        val client = buildClient(instance.token, instance.trustSelfSigned, instance.id)
        val retrofit = buildComponentRetrofit(client, instance.baseUrl, "/api/gateway/v1/")
        val platformService = retrofit.create(PlatformApiService::class.java)

        val controllerService = cached?.controller ?: run {
            val apiVersion = resolveApiVersion(instance.apiVersion)
            buildRetrofit(client, instance.baseUrl, apiVersion)
                .create(AapApiService::class.java)
        }
        serviceCache[instance.id] = (cached ?: ServiceCache(controller = controllerService))
            .copy(platform = platformService)
        return platformService
    }

    @Synchronized
    override fun evictInstance(instanceId: String) {
        serviceCache.remove(instanceId)
    }

    private fun resolveApiVersion(apiVersionStr: String): ApiVersion = try {
        ApiVersion.valueOf(apiVersionStr)
    } catch (_: Exception) {
        ApiVersion.CONTROLLER_V2
    }

    private fun buildClient(token: String, trustSelfSigned: Boolean, instanceId: String): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(
                tokenProvider = { tokenManager.activeInstance.value?.token ?: token },
                instanceIdProvider = { instanceId }
            ))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
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

    private fun buildComponentRetrofit(client: OkHttpClient, baseUrl: String, basePath: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${baseUrl.trimEnd('/')}$basePath")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
