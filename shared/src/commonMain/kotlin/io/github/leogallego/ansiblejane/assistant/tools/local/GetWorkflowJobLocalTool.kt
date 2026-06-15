package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.WorkflowRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetWorkflowJobLocalTool(
    private val repository: WorkflowRepository
) : AapLocalTool<GetWorkflowJobLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_workflow_job", "Get status of a workflow job by ID, including its node details"
) {
    @Serializable
    data class Args(
        @SerialName("workflow_job_id")
        @property:LLMDescription("ID of the workflow job")
        val workflowJobId: Int
    )

    override suspend fun execute(args: Args): String {
        val job = repository.getWorkflowJobStatus(args.workflowJobId).getOrThrow()
        val nodes = repository.getWorkflowNodes(args.workflowJobId).getOrElse { emptyList() }
        return networkJson.encodeToString(mapOf(
            "workflow_job" to networkJson.encodeToString(job),
            "nodes" to networkJson.encodeToString(nodes)
        ))
    }
}
