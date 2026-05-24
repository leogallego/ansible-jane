package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaDecisionEnvironment(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("organization_id") val organizationId: Int? = null
)
