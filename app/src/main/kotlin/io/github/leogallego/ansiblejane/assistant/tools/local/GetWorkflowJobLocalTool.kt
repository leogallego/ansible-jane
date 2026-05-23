package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.WorkflowRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetWorkflowJobLocalTool(
    private val repository: WorkflowRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_workflow_job",
        description = "Get status of a workflow job by ID, including its node details",
        parametersSchema = buildToolSchema(
            Triple("workflow_job_id", "integer", "ID of the workflow job"),
            required = listOf("workflow_job_id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val jobId = args.intArg("workflow_job_id")
            ?: return@executeSafely ToolResult(success = false, data = "workflow_job_id is required", errorType = ErrorType.NOT_FOUND)
        val job = repository.getWorkflowJobStatus(jobId).getOrThrow()
        val nodes = repository.getWorkflowNodes(jobId).getOrElse { emptyList() }
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "workflow_job" to networkJson.encodeToString(job),
                "nodes" to networkJson.encodeToString(nodes)
            ))
        )
    }
}
