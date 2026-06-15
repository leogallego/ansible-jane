package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.WorkflowRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class LaunchWorkflowLocalTool(
    private val repository: WorkflowRepository
) : AapLocalTool<LaunchWorkflowLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "launch_workflow",
    description = "Launch a workflow job template by ID with optional extra variables",
    isDestructive = true
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ID of the workflow template to launch")
        @SerialName("template_id")
        val templateId: Int,
        @property:LLMDescription("Extra variables as YAML/JSON string")
        @SerialName("extra_vars")
        val extraVars: String? = null
    )

    override suspend fun execute(args: Args): String {
        val jobId = repository.launchWorkflow(args.templateId, args.extraVars).getOrThrow()
        return """{"workflow_job_id": $jobId, "status": "launched"}"""
    }
}
