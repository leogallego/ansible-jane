package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    val id: Int,
    val name: String,
    val description: String = "",
    val kind: String = "",
    @SerialName("total_hosts") val totalHosts: Int = 0,
    @SerialName("total_groups") val totalGroups: Int = 0,
    @SerialName("has_inventory_sources") val hasInventorySources: Boolean = false,
    val variables: String = "",
    val created: String = "",
    val modified: String = "",
    @SerialName("summary_fields") val summaryFields: InventorySummaryFields = InventorySummaryFields()
) {
    val displayKind: String
        get() = when (kind) {
            "smart" -> "Smart"
            "constructed" -> "Constructed"
            else -> "Regular"
        }
}

@Serializable
data class InventorySummaryFields(
    val organization: OrganizationSummary? = null
)

@Serializable
data class OrganizationSummary(
    val id: Int,
    val name: String
)
