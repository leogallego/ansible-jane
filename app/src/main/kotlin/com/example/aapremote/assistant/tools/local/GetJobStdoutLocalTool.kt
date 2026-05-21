package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.JobRepository
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val jobId = args.intArg("job_id")
            ?: return@executeSafely ToolResult(success = false, data = "job_id is required", errorType = ErrorType.NOT_FOUND)
        val stdout = repository.getJobStdout(jobId).getOrThrow()
        ToolResult(success = true, data = stdout)
    }
}
