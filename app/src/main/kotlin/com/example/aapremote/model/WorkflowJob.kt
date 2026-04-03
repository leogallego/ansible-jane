package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowJob(
    val id: Int,
    val name: String = "",
    val status: JobStatus,
    val failed: Boolean = false,
    val started: String? = null,
    val finished: String? = null,
    val elapsed: Double? = null,
    @SerialName("summary_fields") val summaryFields: WorkflowJobSummaryFields = WorkflowJobSummaryFields()
) {
    val workflowJobTemplateName: String
        get() = summaryFields.workflowJobTemplate?.name ?: name
}

@Serializable
data class WorkflowJobSummaryFields(
    @SerialName("workflow_job_template") val workflowJobTemplate: WorkflowJobTemplateRef? = null
)

@Serializable
data class WorkflowJobTemplateRef(
    val id: Int,
    val name: String
)
