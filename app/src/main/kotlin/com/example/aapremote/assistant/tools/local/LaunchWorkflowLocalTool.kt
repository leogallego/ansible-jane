package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.WorkflowRepository

class LaunchWorkflowLocalTool(
    private val repository: WorkflowRepository
) : LocalTool(
    spec = ToolSpec(
        name = "launch_workflow",
        description = "Launch a workflow job template by ID with optional extra variables",
        parametersSchema = buildToolSchema(
            Triple("template_id", "integer", "ID of the workflow template to launch"),
            Triple("extra_vars", "string", "Extra variables as YAML/JSON string"),
            required = listOf("template_id")
        )
    ),
    destructive = true
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val templateId = (args["template_id"] as? Number)?.toInt()
                ?: return ToolResult(success = false, data = "template_id is required", errorType = ErrorType.NOT_FOUND)
            val extraVars = args["extra_vars"] as? String
            val jobId = repository.launchWorkflow(templateId, extraVars).getOrThrow()
            ToolResult(success = true, data = """{"workflow_job_id": $jobId, "status": "launched"}""")
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}
