package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowApproval(
    val id: Int,
    val name: String = "",
    val status: String = "",
    @SerialName("workflow_job") val workflowJob: Int? = null,
    @SerialName("summary_fields") val summaryFields: WorkflowApprovalSummary = WorkflowApprovalSummary(),
    val created: String = "",
    @SerialName("timed_out") val timedOut: Boolean = false,
)

@Serializable
data class WorkflowApprovalSummary(
    @SerialName("workflow_job") val workflowJob: WorkflowApprovalJobRef? = null,
    @SerialName("workflow_job_template") val workflowJobTemplate: WorkflowApprovalTemplateRef? = null,
    @SerialName("approved_or_denied_by") val approvedOrDeniedBy: WorkflowApprovalUserRef? = null,
)

@Serializable
data class WorkflowApprovalJobRef(
    val id: Int,
    val name: String = "",
    val status: String = "",
    @SerialName("launched_by") val launchedBy: WorkflowApprovalUserRef? = null,
)

@Serializable
data class WorkflowApprovalTemplateRef(
    val id: Int,
    val name: String = "",
)

@Serializable
data class WorkflowApprovalUserRef(
    val id: Int,
    val username: String = "",
)
