package com.example.aapremote.data

import com.example.aapremote.model.User
import com.example.aapremote.network.AapApiService
import com.example.aapremote.network.ApiVersion
import com.example.aapremote.network.ApiVersionDetector
import com.example.aapremote.network.AuthInterceptor
import com.example.aapremote.network.CertTrustManager
import com.example.aapremote.network.networkJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthRepository(
    private val tokenManager: TokenManager
) {

    suspend fun validateCredentials(
        baseUrl: String,
        token: String,
        trustSelfSigned: Boolean
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val client = buildClient(token, trustSelfSigned)
            val detector = ApiVersionDetector(client)
            val apiVersion = detector.detect(baseUrl, token)
            val api = buildApi(client, baseUrl, apiVersion)
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return@withContext Result.failure(Exception("No user data returned"))

            tokenManager.saveCredentials(
                baseUrl = baseUrl,
                token = token,
                apiVersion = apiVersion,
                trustSelfSigned = trustSelfSigned
            )

            Result.success(user)
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                401 -> "Invalid token. Please check your Personal Access Token."
                403 -> "Access denied. Your token lacks required permissions."
                else -> "Server error (${e.code()}): ${e.message()}"
            }
            Result.failure(Exception(message))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Cannot reach server. Check the URL and your network connection."))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Connection refused. Verify the AAP server is running."))
        } catch (e: javax.net.ssl.SSLException) {
            Result.failure(Exception("SSL error. If using a self-signed certificate, enable the toggle."))
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    suspend fun checkExistingCredentials(): Result<User>? = withContext(Dispatchers.IO) {
        if (!tokenManager.loadCredentials()) return@withContext null
        val baseUrl = tokenManager.cachedBaseUrl ?: return@withContext null
        val token = tokenManager.cachedToken ?: return@withContext null
        val trustSelfSigned = tokenManager.cachedTrustSelfSigned

        try {
            val client = buildClient(token, trustSelfSigned)
            val api = buildApi(client, baseUrl, tokenManager.cachedApiVersion)
            val response = api.getMe()
            val user = response.results.firstOrNull()
                ?: return@withContext Result.failure(Exception("No user data returned"))
            Result.success(user)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout() {
        tokenManager.clearCredentials()
    }

    fun isLoggedIn() = tokenManager.isLoggedIn

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
