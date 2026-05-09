package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.WorkflowRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

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
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val jobId = (args["workflow_job_id"] as? Number)?.toInt()
                ?: return ToolResult(success = false, data = "workflow_job_id is required", errorType = ErrorType.NOT_FOUND)
            val job = repository.getWorkflowJobStatus(jobId).getOrThrow()
            val nodes = repository.getWorkflowNodes(jobId).getOrElse { emptyList() }
            ToolResult(
                success = true,
                data = networkJson.encodeToString(mapOf(
                    "workflow_job" to networkJson.encodeToString(job),
                    "nodes" to networkJson.encodeToString(nodes)
                ))
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}
