package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.JobRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetJobLocalTool(
    private val repository: JobRepository
) : AapLocalTool<GetJobLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_job", "Get status and details of a specific job by ID"
) {
    @Serializable
    data class Args(
        @SerialName("job_id")
        @property:LLMDescription("ID of the job to retrieve")
        val jobId: Int
    )

    override suspend fun execute(args: Args): String {
        val job = repository.getJobStatus(args.jobId).getOrThrow()
        return networkJson.encodeToString(job)
    }
}
