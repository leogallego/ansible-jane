package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaProject(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val url: String = "",
    @SerialName("scm_branch") val scmBranch: String = "",
    @SerialName("organization_id") val organizationId: Int? = null
)
