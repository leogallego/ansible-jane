package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.User
import io.github.leogallego.ansiblejane.network.AapApiService
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.network.ApiVersion
import io.github.leogallego.ansiblejane.network.ApiVersionDetector
import io.github.leogallego.ansiblejane.network.AuthInterceptor
import io.github.leogallego.ansiblejane.network.CertTrustManager
import io.github.leogallego.ansiblejane.network.InstanceDiscovery
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthRepository(
    private val tokenManager: ITokenManager,
    private val apiProvider: IAapApiProvider,
    private val instanceDiscovery: InstanceDiscovery
) : IAuthRepository {

    override suspend fun validateCredentials(
        baseUrl: String,
        token: String,
        trustSelfSigned: Boolean,
        alias: String?,
        existingInstanceId: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val client = buildClient(token, trustSelfSigned)
            val detector = ApiVersionDetector(client)
            val apiVersion = detector.detect(baseUrl, token)
            val api = buildApi(client, baseUrl, apiVersion)
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return@withContext Result.failure(Exception("No user data returned"))

            val instanceId = tokenManager.saveCredentials(
                baseUrl = baseUrl,
                token = token,
                apiVersion = apiVersion,
                trustSelfSigned = trustSelfSigned,
                alias = alias,
                existingId = existingInstanceId
            )

            // Evict cached service so it rebuilds with the new token/settings
            apiProvider.evictInstance(instanceId)

            // Discover instance capabilities (version, components, platform type)
            runDiscovery(instanceId, baseUrl, token, apiVersion, client)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reAuthenticate(instanceId: String, newToken: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val instance = tokenManager.getInstanceById(instanceId)
                    ?: return@withContext Result.failure(Exception("Instance not found"))

                val trustSelfSigned = instance.trustSelfSigned
                val client = buildClient(newToken, trustSelfSigned)
                val apiVersion = try {
                    ApiVersion.valueOf(instance.apiVersion)
                } catch (_: Exception) {
                    ApiVersion.CONTROLLER_V2
                }
                val api = buildApi(client, instance.baseUrl, apiVersion)
                val response = api.getMe()
                val user = response.results.firstOrNull()
                    ?: return@withContext Result.failure(Exception("No user data returned"))

                tokenManager.saveCredentials(
                    baseUrl = instance.baseUrl,
                    token = newToken,
                    apiVersion = apiVersion,
                    trustSelfSigned = trustSelfSigned,
                    alias = instance.alias,
                    existingId = instanceId
                )

                // Evict cached service so it rebuilds with the new token
                apiProvider.evictInstance(instanceId)

                // Re-discover instance capabilities
                runDiscovery(instanceId, instance.baseUrl, newToken, apiVersion, client)

                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun checkExistingCredentials(): Result<User>? = withContext(Dispatchers.IO) {
        if (!tokenManager.loadCredentials()) return@withContext null
        val activeInstance = tokenManager.activeInstance.value ?: return@withContext null

        try {
            val client = buildClient(activeInstance.token, activeInstance.trustSelfSigned)
            val apiVersion = try {
                ApiVersion.valueOf(activeInstance.apiVersion)
            } catch (_: Exception) {
                ApiVersion.CONTROLLER_V2
            }
            val api = buildApi(client, activeInstance.baseUrl, apiVersion)
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return@withContext Result.failure(Exception("No user data returned"))
            Result.success(user)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun logoutInstance(instanceId: String) {
        tokenManager.removeInstance(instanceId)
    }

    override suspend fun logout() {
        tokenManager.clearCredentials()
    }

    override fun isLoggedIn() = tokenManager.isLoggedIn

    private suspend fun runDiscovery(
        instanceId: String,
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        client: OkHttpClient
    ) {
        try {
            val info = instanceDiscovery.discover(baseUrl, token, apiVersion, client)
            tokenManager.updateInstanceInfo(instanceId, info)
        } catch (_: Exception) {
            // Discovery is best-effort — don't fail the auth flow
        }
    }

    private fun buildClient(token: String, trustSelfSigned: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
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

    private fun buildApi(client: OkHttpClient, baseUrl: String, apiVersion: ApiVersion): AapApiService {
        return Retrofit.Builder()
            .baseUrl("${baseUrl.trimEnd('/')}${apiVersion.prefix}")
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AapApiService::class.java)
    }
}
