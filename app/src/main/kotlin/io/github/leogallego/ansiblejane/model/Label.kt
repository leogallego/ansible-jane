package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val id: Int,
    val name: String
)
