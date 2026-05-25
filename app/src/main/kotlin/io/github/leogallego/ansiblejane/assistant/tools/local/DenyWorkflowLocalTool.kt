package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DenyWorkflowLocalTool(
    private val repository: IWorkflowRepository
) : AapLocalTool<DenyWorkflowLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "deny_workflow",
    description = "Deny a pending workflow approval step",
    destructive = true
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ID of the workflow approval to deny")
        @SerialName("approval_id")
        val approvalId: Int
    )

    override suspend fun execute(args: Args): String {
        repository.denyWorkflow(args.approvalId).getOrThrow()
        return """{"approval_id": ${args.approvalId}, "status": "denied"}"""
    }
}
