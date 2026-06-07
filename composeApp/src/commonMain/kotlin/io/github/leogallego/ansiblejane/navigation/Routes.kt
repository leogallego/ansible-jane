package io.github.leogallego.ansiblejane.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data class AuthRoute(
    val mode: String = "",
    val instanceId: String = "",
    val url: String = "",
    val alias: String = "",
    val trustSelfSigned: Boolean = false
)

@Serializable
data object MainRoute

@Serializable
data class JobStatusRoute(val jobId: Int)

@Serializable
data class WorkflowJobStatusRoute(val workflowJobId: Int)

@Serializable
data class WorkflowTemplateDetailRoute(
    val templateId: Int,
    val templateName: String = ""
)

@Serializable
data class ApprovalDetailRoute(val approvalId: Int)

@Serializable
data object SettingsRoute
