package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable

@Serializable
data class AapToken(
    val id: Int,
    val description: String = "",
    val scope: String = "",
    val user: Int? = null
)
