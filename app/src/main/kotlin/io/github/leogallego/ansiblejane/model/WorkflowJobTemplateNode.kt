package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowJobTemplateNode(
    val id: Int,
    val identifier: String = "",
    @SerialName("unified_job_template") val unifiedJobTemplate: Int? = null,
    @SerialName("workflow_job_template") val workflowJobTemplate: Int? = null
)
