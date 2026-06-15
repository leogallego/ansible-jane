package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.HostRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetHostJobSummariesLocalTool(
    private val repository: HostRepository
) : AapLocalTool<GetHostJobSummariesLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "get_host_job_summaries",
    description = "Get job execution summaries for a specific host (pass/fail history)"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Host ID")
        val id: Int,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 10, max 20)")
        @SerialName("page_size")
        val pageSize: Int = 10
    )

    override suspend fun execute(args: Args): String {
        val pageSize = args.pageSize.coerceIn(1, 20)
        val result = repository.getHostJobSummaries(
            hostId = args.id,
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "job_host_summaries" to networkJson.encodeToString(result.summaries)
        ))
    }
}
