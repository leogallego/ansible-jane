package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowJobTemplateNode(
    val id: Int,
    val identifier: String = "",
    @SerialName("unified_job_template") val unifiedJobTemplate: Int? = null,
    @SerialName("workflow_job_template") val workflowJobTemplate: Int? = null,
    @SerialName("success_nodes") val successNodes: List<Int> = emptyList(),
    @SerialName("failure_nodes") val failureNodes: List<Int> = emptyList(),
    @SerialName("always_nodes") val alwaysNodes: List<Int> = emptyList()
)
