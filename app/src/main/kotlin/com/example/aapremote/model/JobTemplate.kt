package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JobTemplate(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("ask_variables_on_launch") val askVariablesOnLaunch: Boolean = false,
    val status: String? = null,
    @SerialName("last_job_run") val lastJobRun: String? = null,
    @SerialName("summary_fields") val summaryFields: JobTemplateSummaryFields = JobTemplateSummaryFields()
)

@Serializable
data class JobTemplateSummaryFields(
    val labels: LabelSummary = LabelSummary(),
    @SerialName("user_capabilities") val userCapabilities: UserCapabilities = UserCapabilities()
)

@Serializable
data class LabelSummary(
    val count: Int = 0,
    val results: List<Label> = emptyList()
)

@Serializable
data class UserCapabilities(
    val start: Boolean = false
)
