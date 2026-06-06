package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.data.ITokenManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private data class ClientGroup(
    val controller: AapApiClient,
    val eda: EdaApiClient? = null,
    val platform: PlatformApiClient? = null
)

class HttpClientFactory(
    private val tokenManager: ITokenManager,
    private val json: Json,
    private val logLevel: LogLevel = LogLevel.NONE
) : IAapApiProvider {
    private val clientCache = mutableMapOf<String, ClientGroup>()

    @Synchronized
    override fun getApiService(): AapApiClient {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = clientCache[instance.id]
        if (cached != null) return cached.controller

        val apiVersion = resolveApiVersion(instance.apiVersion)
        val client = buildClient(instance.baseUrl, apiVersion.prefix, instance.trustSelfSigned, instance.id)
        val apiClient = AapApiClient(client)
        clientCache[instance.id] = ClientGroup(controller = apiClient)
        return apiClient
    }

    @Synchronized
    override fun getEdaApiService(): EdaApiClient {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = clientCache[instance.id]
        if (cached?.eda != null) return cached.eda

        val client = buildClient(instance.baseUrl, "/api/eda/v1/", instance.trustSelfSigned, instance.id)
        val edaClient = EdaApiClient(client)

        val controllerClient = cached?.controller ?: run {
            val apiVersion = resolveApiVersion(instance.apiVersion)
            AapApiClient(buildClient(instance.baseUrl, apiVersion.prefix, instance.trustSelfSigned, instance.id))
        }
        clientCache[instance.id] = (cached ?: ClientGroup(controller = controllerClient))
            .copy(eda = edaClient)
        return edaClient
    }

    @Synchronized
    override fun getPlatformApiService(): PlatformApiClient {
        val instance = tokenManager.activeInstance.value
            ?: throw IllegalStateException("No active AAP instance. Please log in first.")

        val cached = clientCache[instance.id]
        if (cached?.platform != null) return cached.platform

        val client = buildClient(instance.baseUrl, "/api/gateway/v1/", instance.trustSelfSigned, instance.id)
        val platformClient = PlatformApiClient(client)

        val controllerClient = cached?.controller ?: run {
            val apiVersion = resolveApiVersion(instance.apiVersion)
            AapApiClient(buildClient(instance.baseUrl, apiVersion.prefix, instance.trustSelfSigned, instance.id))
        }
        clientCache[instance.id] = (cached ?: ClientGroup(controller = controllerClient))
            .copy(platform = platformClient)
        return platformClient
    }

    @Synchronized
    override fun evictInstance(instanceId: String) {
        clientCache.remove(instanceId)
    }

    private fun resolveApiVersion(apiVersionStr: String): ApiVersion = try {
        ApiVersion.valueOf(apiVersionStr)
    } catch (_: Exception) {
        ApiVersion.CONTROLLER_V2
    }

    private fun buildClient(
        baseUrl: String,
        basePath: String,
        trustSelfSigned: Boolean,
        instanceId: String
    ): HttpClient = createPlatformHttpClient(trustSelfSigned) {
        expectSuccess = true

        install(ContentNegotiation) { json(json) }

        install(Logging) {
            level = logLevel
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        defaultRequest {
            url("${baseUrl.trimEnd('/')}$basePath")
            val token = tokenManager.activeInstance.value?.token
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 401) {
                    AuthEvents.emitUnauthorized(instanceId)
                }
            }
        }
    }
}
