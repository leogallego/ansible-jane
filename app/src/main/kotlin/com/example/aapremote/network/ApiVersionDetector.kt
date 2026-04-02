package com.example.aapremote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class ApiVersion(val prefix: String) {
    V2("/api/v2/"),
    CONTROLLER_V2("/api/controller/v2/")
}

class ApiVersionDetector(private val httpClient: OkHttpClient) {

    suspend fun detect(baseUrl: String, token: String): ApiVersion = withContext(Dispatchers.IO) {
        val normalizedUrl = baseUrl.trimEnd('/')
        try {
            val request = Request.Builder()
                .url("$normalizedUrl${ApiVersion.CONTROLLER_V2.prefix}me/")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            response.close()
            if (response.code != 404) ApiVersion.CONTROLLER_V2 else ApiVersion.V2
        } catch (_: Exception) {
            ApiVersion.V2
        }
    }
}
