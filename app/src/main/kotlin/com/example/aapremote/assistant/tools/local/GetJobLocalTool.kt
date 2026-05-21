package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.JobRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetJobLocalTool(
    private val repository: JobRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_job",
        description = "Get status and details of a specific job by ID",
        parametersSchema = buildToolSchema(
            Triple("job_id", "integer", "ID of the job to retrieve"),
            required = listOf("job_id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val jobId = args.intArg("job_id")
            ?: return@executeSafely ToolResult(success = false, data = "job_id is required", errorType = ErrorType.NOT_FOUND)
        val job = repository.getJobStatus(jobId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(job))
    }
}
