package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.JobRepository

class GetJobStdoutLocalTool(
    private val repository: JobRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_job_stdout",
        description = "Get stdout output of a completed job by ID",
        parametersSchema = buildToolSchema(
            Triple("job_id", "integer", "ID of the job"),
            required = listOf("job_id")
        )
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val jobId = (args["job_id"] as? Number)?.toInt()
                ?: return ToolResult(success = false, data = "job_id is required", errorType = ErrorType.NOT_FOUND)
            val stdout = repository.getJobStdout(jobId).getOrThrow()
            ToolResult(success = true, data = stdout)
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}
