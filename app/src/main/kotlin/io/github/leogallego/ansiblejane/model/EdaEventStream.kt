package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaEventStream(
    val id: Int,
    val name: String = "",
    val uuid: String = "",
    @SerialName("forward_events") val forwardEvents: Boolean = false,
    @SerialName("organization_id") val organizationId: Int? = null
)
