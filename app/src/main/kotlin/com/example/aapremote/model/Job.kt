package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: Int,
    val name: String = "",
    val status: JobStatus,
    val failed: Boolean = false,
    val started: String? = null,
    val finished: String? = null,
    val elapsed: Double? = null,
    @SerialName("launch_type") val launchType: String = "",
    @SerialName("summary_fields") val summaryFields: JobSummaryFields = JobSummaryFields()
) {
    val jobTemplateId: Int?
        get() = summaryFields.jobTemplate?.id

    val jobTemplateName: String
        get() = summaryFields.jobTemplate?.name ?: name
}

@Serializable
data class JobSummaryFields(
    @SerialName("job_template") val jobTemplate: JobTemplateRef? = null
)

@Serializable
data class JobTemplateRef(
    val id: Int,
    val name: String
)
