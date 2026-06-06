package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable

@Serializable
data class EdaCredentialType(
    val id: Int,
    val name: String = "",
    val description: String = ""
)
