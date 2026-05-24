package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlatformOrganization(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class PlatformUser(
    val id: Int,
    val username: String = "",
    val email: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("is_superuser") val isSuperuser: Boolean = false,
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class PlatformTeam(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val organization: Int? = null,
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class PlatformRoleDefinition(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("content_type") val contentType: String? = null,
    val permissions: List<String> = emptyList(),
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class PlatformAuthenticator(
    val id: Int,
    val name: String = "",
    val enabled: Boolean = true,
    val type: String = "",
    @SerialName("create_objects") val createObjects: Boolean = false,
    val order: Int = 0,
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class PlatformService(
    val id: Int,
    val name: String = "",
    @SerialName("service_cluster") val serviceCluster: Int? = null,
    @SerialName("api_slug") val apiSlug: String = "",
    val created: String = "",
    val modified: String = ""
)

@Serializable
data class PlatformServiceCluster(
    val id: Int,
    val name: String = "",
    @SerialName("service_type") val serviceType: String = "",
    val created: String = "",
    val modified: String = ""
)
