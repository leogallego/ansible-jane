package com.example.aapremote.model

import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val id: Int,
    val name: String
)
