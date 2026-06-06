package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("max_hosts") val maxHosts: Int = 0
)
