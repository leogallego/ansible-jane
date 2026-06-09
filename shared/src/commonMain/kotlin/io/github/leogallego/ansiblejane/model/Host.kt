package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Host(
    val id: Int,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val variables: String = "",
    @SerialName("has_active_failures") val hasActiveFailures: Boolean = false,
    val inventory: Int = 0,
    val created: String = "",
    val modified: String = "",
    @SerialName("last_job") val lastJob: Int? = null,
    @SerialName("last_job_host_summary") val lastJobHostSummary: Int? = null,
    @SerialName("summary_fields") val summaryFields: HostSummaryFields = HostSummaryFields()
)

@Serializable
data class HostSummaryFields(
    val inventory: InventorySummary? = null,
    val groups: GroupsSummary? = null
)

@Serializable
data class InventorySummary(
    val id: Int,
    val name: String
)

@Serializable
data class GroupsSummary(
    val count: Int = 0,
    val results: List<GroupSummary> = emptyList()
)

@Serializable
data class GroupSummary(
    val id: Int,
    val name: String
)

@Serializable
data class HostFacts(
    @SerialName("ansible_facts") val ansibleFacts: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class JobHostSummary(
    val id: Int,
    val job: Int,
    val host: Int,
    val failed: Boolean = false,
    val ok: Int = 0,
    val changed: Int = 0,
    val failures: Int = 0,
    val skipped: Int = 0,
    val created: String = "",
    @SerialName("summary_fields") val summaryFields: JobHostSummaryFields = JobHostSummaryFields()
)

@Serializable
data class JobHostSummaryFields(
    val job: JobSummaryRef? = null
)

@Serializable
data class JobSummaryRef(
    val id: Int,
    val name: String = "",
    val status: String = ""
)
