package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaRulebook(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("project_id") val projectId: Int? = null,
    @SerialName("organization_id") val organizationId: Int? = null
)
