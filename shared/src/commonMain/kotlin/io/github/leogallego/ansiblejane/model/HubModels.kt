package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GalaxyV3Response<T>(
    val meta: GalaxyV3Meta = GalaxyV3Meta(),
    val links: GalaxyV3Links = GalaxyV3Links(),
    val data: List<T> = emptyList()
)

@Serializable
data class GalaxyV3Meta(
    val count: Int = 0
)

@Serializable
data class GalaxyV3Links(
    val first: String? = null,
    val previous: String? = null,
    val next: String? = null,
    val last: String? = null
)

@Serializable
data class HubCollection(
    val href: String = "",
    val namespace: String = "",
    val name: String = "",
    val description: String = "",
    val deprecated: Boolean = false,
    @SerialName("highest_version") val highestVersion: HubCollectionVersionSummary? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class HubCollectionVersionSummary(
    val version: String = "",
    val href: String = ""
)

@Serializable
data class HubNamespace(
    val name: String = "",
    val company: String = "",
    val description: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class HubCollectionVersion(
    val namespace: String = "",
    val name: String = "",
    val version: String = "",
    val status: String = "",
    val repository: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class HubEeRepository(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class HubEeRegistry(
    val id: Int = 0,
    val name: String = "",
    val url: String = "",
    @SerialName("download_concurrency") val downloadConcurrency: Int = 0,
    @SerialName("rate_limit") val rateLimit: Int = 0,
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class HubUser(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("is_superuser") val isSuperuser: Boolean = false
)

@Serializable
data class HubGroup(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class HubRoleDefinition(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val permissions: List<String> = emptyList(),
    @SerialName("content_type") val contentType: String? = null,
    val managed: Boolean = false
)
