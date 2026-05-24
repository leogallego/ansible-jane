package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log
import io.github.leogallego.ansiblejane.model.AapComponent
import io.github.leogallego.ansiblejane.model.InstanceInfo
import io.github.leogallego.ansiblejane.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class InstanceDiscovery(private val json: Json) {

    companion object {
        private const val TAG = "InstanceDiscovery"

        // Controller version → AAP version mapping
        // Controller 4.5.x → AAP 2.5, Controller 4.6.x → AAP 2.6, etc.
        private val CONTROLLER_TO_AAP_VERSION = mapOf(
            "4.5" to "2.5",
            "4.6" to "2.6",
            "4.7" to "2.7"
        )
    }

    suspend fun discover(
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        httpClient: OkHttpClient
    ): InstanceInfo = withContext(Dispatchers.IO) {
        val normalizedUrl = baseUrl.trimEnd('/')
        val components = mutableSetOf(AapComponent.CONTROLLER)

        // Run probes in parallel
        coroutineScope {
            val pingDeferred = async { probePing(httpClient, normalizedUrl, apiVersion, token) }
            val configDeferred = async { probeConfig(httpClient, normalizedUrl, apiVersion, token) }
            val gatewayDeferred = async { probeEndpoint(httpClient, "$normalizedUrl/api/gateway/v1/", token) }
            val edaDeferred = async { probeEndpoint(httpClient, "$normalizedUrl/api/eda/v1/users/me/", token) }
            val hubDeferred = async { probeEndpoint(httpClient, "$normalizedUrl/api/galaxy/_ui/v1/me/", token) }

            val pingResult = pingDeferred.await()
            val configResult = configDeferred.await()
            val hasGateway = gatewayDeferred.await()
            val hasEda = edaDeferred.await()
            val hasHub = hubDeferred.await()

            if (hasGateway) components.add(AapComponent.GATEWAY)
            if (hasEda) components.add(AapComponent.EDA)
            if (hasHub) components.add(AapComponent.HUB)

            val controllerVersion = pingResult?.version ?: ""
            val hasLicense = configResult?.hasLicense ?: false

            val platformType = derivePlatformType(hasGateway, hasLicense)
            val aapVersion = deriveAapVersion(controllerVersion, platformType)

            Log.d(TAG, "Discovery: version=$controllerVersion, platform=$platformType, " +
                "aap=$aapVersion, components=$components")

            InstanceInfo(
                controllerVersion = controllerVersion,
                platformType = platformType.name,
                aapVersion = aapVersion,
                components = components.map { it.name }
            )
        }
    }

    private fun derivePlatformType(hasGateway: Boolean, hasLicense: Boolean): PlatformType = when {
        hasGateway && hasLicense -> PlatformType.AAP
        hasGateway && !hasLicense -> PlatformType.JEWEL
        !hasGateway && !hasLicense -> PlatformType.AWX
        !hasGateway && hasLicense -> PlatformType.AAP
        else -> PlatformType.UNKNOWN
    }

    private fun deriveAapVersion(controllerVersion: String, platformType: PlatformType): String? {
        if (platformType != PlatformType.AAP || controllerVersion.isBlank()) return null
        val majorMinor = controllerVersion.split(".").take(2).joinToString(".")
        return CONTROLLER_TO_AAP_VERSION[majorMinor]
    }

    private data class PingResult(val version: String)

    private fun probePing(
        client: OkHttpClient,
        baseUrl: String,
        apiVersion: ApiVersion,
        token: String
    ): PingResult? = try {
        val url = "$baseUrl${apiVersion.prefix}ping/"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        response.close()

        if (response.isSuccessful && body != null) {
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val version = jsonObj["version"]?.jsonPrimitive?.content ?: ""
            PingResult(version)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.d(TAG, "Ping probe failed: ${e.message}")
        null
    }

    private data class ConfigResult(val hasLicense: Boolean)

    private fun probeConfig(
        client: OkHttpClient,
        baseUrl: String,
        apiVersion: ApiVersion,
        token: String
    ): ConfigResult? = try {
        val url = "$baseUrl${apiVersion.prefix}config/"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string()
        response.close()

        if (response.isSuccessful && body != null) {
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val licenseInfo = jsonObj["license_info"]
            val hasLicense = when {
                licenseInfo == null -> false
                licenseInfo !is JsonObject -> false
                licenseInfo.jsonObject.isEmpty() -> false
                licenseInfo.jsonObject["license_type"]?.jsonPrimitive?.content == "open" -> false
                else -> true
            }
            ConfigResult(hasLicense)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.d(TAG, "Config probe failed: ${e.message}")
        null
    }

    private fun probeEndpoint(
        client: OkHttpClient,
        url: String,
        token: String
    ): Boolean = try {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        val response = client.newCall(request).execute()
        response.close()
        response.isSuccessful
    } catch (e: Exception) {
        Log.d(TAG, "Probe $url failed: ${e.message}")
        false
    }
}
