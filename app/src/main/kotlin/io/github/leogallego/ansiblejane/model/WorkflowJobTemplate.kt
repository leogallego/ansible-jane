package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowJobTemplate(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("ask_variables_on_launch") val askVariablesOnLaunch: Boolean = false,
    val status: String? = null,
    @SerialName("last_job_run") val lastJobRun: String? = null,
    @SerialName("summary_fields") val summaryFields: WorkflowTemplateSummaryFields = WorkflowTemplateSummaryFields()
)

@Serializable
data class WorkflowTemplateSummaryFields(
    val labels: LabelSummary = LabelSummary(),
    @SerialName("user_capabilities") val userCapabilities: UserCapabilities = UserCapabilities()
)
