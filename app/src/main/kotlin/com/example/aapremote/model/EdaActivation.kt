package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaActivation(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val status: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("restart_policy") val restartPolicy: String = "",
    @SerialName("rulebook_name") val rulebookName: String? = null,
    @SerialName("decision_environment_name") val decisionEnvironmentName: String? = null,
    @SerialName("project_name") val projectName: String? = null,
    @SerialName("organization_name") val organizationName: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
