package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoleDefinition(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("content_type") val contentType: String = "",
    val permissions: List<String> = emptyList()
)
