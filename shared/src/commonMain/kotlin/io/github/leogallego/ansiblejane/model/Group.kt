package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val inventory: Int? = null
)
