package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.User
import io.github.leogallego.ansiblejane.network.AapApiClient
import io.github.leogallego.ansiblejane.network.ApiVersion
import io.github.leogallego.ansiblejane.network.ApiVersionDetector
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.network.InstanceDiscovery
import io.github.leogallego.ansiblejane.network.createPlatformHttpClient
import io.github.leogallego.ansiblejane.network.networkJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    ): Result<User> {
        return try {
            val detector = ApiVersionDetector()
            val apiVersion = detector.detect(baseUrl, token, trustSelfSigned)
            val api = buildApiClient(baseUrl, apiVersion, token, trustSelfSigned)
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return Result.failure(Exception("No user data returned"))

            val instanceId = tokenManager.saveCredentials(
                baseUrl = baseUrl,
                token = token,
                apiVersion = apiVersion,
                trustSelfSigned = trustSelfSigned,
                alias = alias,
                existingId = existingInstanceId
            )

            apiProvider.evictInstance(instanceId)
            launchDiscovery(instanceId, baseUrl, token, apiVersion, trustSelfSigned)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reAuthenticate(instanceId: String, newToken: String): Result<User> {
        return try {
            val instance = tokenManager.getInstanceById(instanceId)
                ?: return Result.failure(Exception("Instance not found"))

            val trustSelfSigned = instance.trustSelfSigned
            val apiVersion = try {
                ApiVersion.valueOf(instance.apiVersion)
            } catch (_: Exception) {
                ApiVersion.CONTROLLER_V2
            }
            val api = buildApiClient(instance.baseUrl, apiVersion, newToken, trustSelfSigned)
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return Result.failure(Exception("No user data returned"))

            tokenManager.saveCredentials(
                baseUrl = instance.baseUrl,
                token = newToken,
                apiVersion = apiVersion,
                trustSelfSigned = trustSelfSigned,
                alias = instance.alias,
                existingId = instanceId
            )

            apiProvider.evictInstance(instanceId)
            launchDiscovery(instanceId, instance.baseUrl, newToken, apiVersion, trustSelfSigned)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkExistingCredentials(): CredentialStatus {
        if (!tokenManager.loadCredentials()) return CredentialStatus.NoCredentials
        val activeInstance = tokenManager.activeInstance.value
            ?: return CredentialStatus.NoCredentials

        return try {
            val apiVersion = try {
                ApiVersion.valueOf(activeInstance.apiVersion)
            } catch (_: Exception) {
                ApiVersion.CONTROLLER_V2
            }
            val api = buildApiClient(
                activeInstance.baseUrl, apiVersion,
                activeInstance.token, activeInstance.trustSelfSigned
            )
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return CredentialStatus.ValidationFailed(
                    Exception("No user data returned")
                )
            CredentialStatus.Valid(user)
        } catch (e: Exception) {
            CredentialStatus.ValidationFailed(e)
        }
    }

    override suspend fun logoutInstance(instanceId: String) {
        tokenManager.removeInstance(instanceId)
    }

    override suspend fun logout() {
        tokenManager.clearCredentials()
    }

    override fun isLoggedIn() = tokenManager.isLoggedIn

    private fun launchDiscovery(
        instanceId: String,
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean
    ) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val info = instanceDiscovery.discover(baseUrl, token, apiVersion, trustSelfSigned)
                tokenManager.updateInstanceInfo(instanceId, info)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Discovery is best-effort
            }
        }
    }

    private fun buildApiClient(
        baseUrl: String,
        apiVersion: ApiVersion,
        token: String,
        trustSelfSigned: Boolean
    ): AapApiClient {
        val client = createPlatformHttpClient(trustSelfSigned) {
            expectSuccess = true
            install(ContentNegotiation) { json(networkJson) }
            defaultRequest {
                url("${baseUrl.trimEnd('/')}${apiVersion.prefix}")
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return AapApiClient(client)
    }
}
