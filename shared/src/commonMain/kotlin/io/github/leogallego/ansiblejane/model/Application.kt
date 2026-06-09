package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Application(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("client_type") val clientType: String = "",
    @SerialName("authorization_grant_type") val authorizationGrantType: String = "",
    val organization: Int? = null
)
