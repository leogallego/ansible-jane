package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.JobRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class GetJobStdoutLocalTool(
    private val repository: JobRepository
) : AapLocalTool<GetJobStdoutLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_job_stdout", "Get stdout output of a completed job by ID"
) {
    @Serializable
    data class Args(
        @SerialName("job_id")
        @property:LLMDescription("ID of the job")
        val jobId: Int
    )

    override suspend fun execute(args: Args): String {
        val stdout = repository.getJobStdout(args.jobId).getOrThrow()
        return stdout
    }
}
