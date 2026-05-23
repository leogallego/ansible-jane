package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowNode(
    val id: Int,
    @SerialName("summary_fields") val summaryFields: WorkflowNodeSummaryFields = WorkflowNodeSummaryFields(),
    @SerialName("do_not_run") val doNotRun: Boolean = false
)

@Serializable
data class WorkflowNodeSummaryFields(
    val job: WorkflowNodeJob? = null
)

@Serializable
data class WorkflowNodeJob(
    val id: Int,
    val name: String,
    val status: JobStatus,
    val type: String = ""
)
