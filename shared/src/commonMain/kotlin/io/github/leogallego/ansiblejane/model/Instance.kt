package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Instance(
    val id: Int,
    val hostname: String = "",
    @SerialName("node_type") val nodeType: String = "",
    val enabled: Boolean = true,
    val capacity: Int = 0,
    @SerialName("consumed_capacity") val consumedCapacity: Float = 0f,
    @SerialName("percent_capacity_remaining") val percentCapacityRemaining: Float = 100f,
    val errors: String = "",
    val version: String = ""
)

@Serializable
data class InstanceGroup(
    val id: Int,
    val name: String = "",
    val instances: Int = 0,
    @SerialName("consumed_capacity") val consumedCapacity: Float = 0f,
    @SerialName("percent_capacity_remaining") val percentCapacityRemaining: Float = 100f,
    @SerialName("is_container_group") val isContainerGroup: Boolean = false
)

@Serializable
data class PingResponse(
    val ha: Boolean = false,
    val version: String = "",
    @SerialName("active_node") val activeNode: String = "",
    @SerialName("install_uuid") val installUuid: String = "",
    val instances: List<PingInstance> = emptyList()
)

@Serializable
data class PingInstance(
    val node: String = "",
    @SerialName("node_type") val nodeType: String = "",
    val capacity: Int = 0,
    val version: String = "",
    val errors: String = ""
)
