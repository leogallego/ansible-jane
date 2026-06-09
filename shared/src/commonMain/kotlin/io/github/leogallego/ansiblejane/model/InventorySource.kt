package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventorySource(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val source: String = "",
    val inventory: Int? = null,
    @SerialName("update_on_launch") val updateOnLaunch: Boolean = false,
    val status: String = ""
)
