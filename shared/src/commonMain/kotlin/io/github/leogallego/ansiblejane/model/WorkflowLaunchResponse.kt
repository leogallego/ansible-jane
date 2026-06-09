package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowLaunchResponse(
    @SerialName("workflow_job") val workflowJob: Int,
    val id: Int,
    val status: String = ""
)
