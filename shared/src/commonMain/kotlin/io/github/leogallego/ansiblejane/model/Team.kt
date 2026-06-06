package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val organization: Int? = null
)
