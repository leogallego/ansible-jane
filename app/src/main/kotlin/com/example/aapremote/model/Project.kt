package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("scm_type") val scmType: String = "",
    @SerialName("scm_url") val scmUrl: String = "",
    @SerialName("scm_branch") val scmBranch: String = "",
    val status: String = "",
    @SerialName("last_job_run") val lastJobRun: String? = null,
    val organization: Int? = null
)

@Serializable
data class ExecutionEnvironment(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val image: String = "",
    val organization: Int? = null,
    val managed: Boolean = false
)
