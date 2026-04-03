package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val rrule: String = "",
    val dtstart: String? = null,
    val dtend: String? = null,
    val timezone: String? = null,
    @SerialName("next_run") val nextRun: String? = null,
    @SerialName("unified_job_template") val unifiedJobTemplate: Int = 0,
    @SerialName("summary_fields") val summaryFields: ScheduleSummaryFields = ScheduleSummaryFields()
) {
    val templateName: String
        get() = summaryFields.unifiedJobTemplate?.name ?: name
}

@Serializable
data class ScheduleSummaryFields(
    @SerialName("unified_job_template") val unifiedJobTemplate: UnifiedJobTemplateRef? = null
)

@Serializable
data class UnifiedJobTemplateRef(
    val id: Int,
    val name: String,
    @SerialName("unified_job_type") val unifiedJobType: String = "job"
)
