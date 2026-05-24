package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CredentialType(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val kind: String = "",
    val managed: Boolean = false,
    val inputs: JsonObject? = null
)
