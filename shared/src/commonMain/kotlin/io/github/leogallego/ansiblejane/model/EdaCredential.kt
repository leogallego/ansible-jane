package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaCredential(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("credential_type_id") val credentialTypeId: Int? = null,
    @SerialName("organization_id") val organizationId: Int? = null
)
