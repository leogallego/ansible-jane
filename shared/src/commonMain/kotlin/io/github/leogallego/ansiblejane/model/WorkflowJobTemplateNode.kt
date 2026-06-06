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
    @SerialName("always_nodes") val alwaysNodes: List<Int> = emptyList(),
    @SerialName("summary_fields") val summaryFields: TemplateNodeSummaryFields = TemplateNodeSummaryFields()
)

@Serializable
data class TemplateNodeSummaryFields(
    @SerialName("unified_job_template") val unifiedJobTemplate: UnifiedJobTemplateSummary? = null
)

@Serializable
data class UnifiedJobTemplateSummary(
    val id: Int = 0,
    val name: String = "",
    @SerialName("unified_job_type") val unifiedJobType: String = ""
)
