package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JobStatus {
    @SerialName("new") NEW,
    @SerialName("pending") PENDING,
    @SerialName("waiting") WAITING,
    @SerialName("running") RUNNING,
    @SerialName("successful") SUCCESSFUL,
    @SerialName("failed") FAILED,
    @SerialName("error") ERROR,
    @SerialName("canceled") CANCELED;

    val isActive: Boolean
        get() = this in setOf(NEW, PENDING, WAITING, RUNNING)

    val isTerminal: Boolean
        get() = !isActive
}
