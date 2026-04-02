package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LaunchRequest(
    @SerialName("extra_vars") val extraVars: String? = null
)

@Serializable
data class LaunchResponse(
    val job: Int,
    val id: Int,
    val status: String = ""
)
