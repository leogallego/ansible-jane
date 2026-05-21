package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.TemplateRepository

class LaunchJobLocalTool(
    private val repository: TemplateRepository
) : LocalTool(
    spec = ToolSpec(
        name = "launch_job",
        description = "Launch a job template by ID with optional extra variables",
        parametersSchema = buildToolSchema(
            Triple("template_id", "integer", "ID of the job template to launch"),
            Triple("extra_vars", "string", "Extra variables as YAML/JSON string"),
            required = listOf("template_id")
        )
    ),
    destructive = true
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult = executeSafely {
        val templateId = (args["template_id"] as? Number)?.toInt()
            ?: return@executeSafely ToolResult(success = false, data = "template_id is required", errorType = ErrorType.NOT_FOUND)
        val extraVars = args["extra_vars"] as? String
        val jobId = repository.launchJob(templateId, extraVars).getOrThrow()
        ToolResult(success = true, data = """{"job_id": $jobId, "status": "launched"}""")
    }
}
