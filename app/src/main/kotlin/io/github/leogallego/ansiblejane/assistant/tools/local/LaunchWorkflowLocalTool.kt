package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.WorkflowRepository
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
