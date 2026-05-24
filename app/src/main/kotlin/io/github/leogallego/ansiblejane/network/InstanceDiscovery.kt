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
    }

    suspend fun discover(
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        httpClient: OkHttpClient
    ): InstanceInfo = withContext(Dispatchers.IO) {
        val normalizedUrl = baseUrl.trimEnd('/')
        val components = mutableSetOf(AapComponent.CONTROLLER)

        coroutineScope {
            val controllerPingDeferred = async { probeVersionEndpoint(httpClient, "$normalizedUrl${apiVersion.prefix}ping/", token) }
            val configDeferred = async { probeConfig(httpClient, normalizedUrl, apiVersion, token) }
            val gatewayPingDeferred = async { probeVersionEndpoint(httpClient, "$normalizedUrl/api/gateway/v1/ping/", token) }
            val edaDeferred = async { probeVersionEndpoint(httpClient, "$normalizedUrl/api/eda/v1/users/me/", token) }
            val hubDeferred = async { probeEndpoint(httpClient, "$normalizedUrl/api/galaxy/_ui/v1/me/", token) }

            val controllerPing = controllerPingDeferred.await()
            val configResult = configDeferred.await()
            val gatewayPing = gatewayPingDeferred.await()
            val edaResult = edaDeferred.await()
            val hasHub = hubDeferred.await()

            val hasGateway = gatewayPing != null
            val hasEda = edaResult != null

            if (hasGateway) components.add(AapComponent.GATEWAY)
            if (hasEda) components.add(AapComponent.EDA)
            if (hasHub) components.add(AapComponent.HUB)

            val controllerVersion = controllerPing?.version ?: ""
            val gatewayVersion = gatewayPing?.version ?: ""
            val edaVersion = edaResult?.version ?: ""
            val hasLicense = configResult?.hasLicense ?: false
            val couldReachController = controllerPing != null || configResult != null

            val platformType = derivePlatformType(hasGateway, hasLicense, couldReachController)
            // AAP version comes from gateway ping (authoritative), not derived from controller
            val aapVersion = gatewayVersion.ifBlank { null }

            Log.d(TAG, "Discovery: controller=$controllerVersion, gateway=$gatewayVersion, " +
                "eda=$edaVersion, platform=$platformType, aap=$aapVersion, components=$components")

            InstanceInfo(
                controllerVersion = controllerVersion,
                gatewayVersion = gatewayVersion,
                edaVersion = edaVersion,
                platformType = platformType.name,
                aapVersion = aapVersion,
                components = components.map { it.name }
            )
        }
    }

    private fun derivePlatformType(
        hasGateway: Boolean,
        hasLicense: Boolean,
        couldReachController: Boolean
    ): PlatformType = when {
        hasLicense -> PlatformType.AAP
        hasGateway -> PlatformType.JEWEL
        couldReachController -> PlatformType.AWX
        else -> PlatformType.UNKNOWN
    }

    private data class VersionResult(val version: String)

    private fun probeVersionEndpoint(
        client: OkHttpClient,
        url: String,
        token: String
    ): VersionResult? = try {
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
            VersionResult(version)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.d(TAG, "Version probe $url failed: ${e.message}")
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
