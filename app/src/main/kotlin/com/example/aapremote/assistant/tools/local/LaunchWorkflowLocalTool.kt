package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.WorkflowRepository
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val templateId = args.intArg("template_id")
            ?: return@executeSafely ToolResult(success = false, data = "template_id is required", errorType = ErrorType.NOT_FOUND)
        val extraVars = args.stringArg("extra_vars")
        val jobId = repository.launchWorkflow(templateId, extraVars).getOrThrow()
        ToolResult(success = true, data = """{"workflow_job_id": $jobId, "status": "launched"}""")
    }
}
