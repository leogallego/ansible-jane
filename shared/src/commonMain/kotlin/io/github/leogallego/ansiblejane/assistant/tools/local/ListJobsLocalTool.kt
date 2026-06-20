package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.listToolJson
import io.github.leogallego.ansiblejane.data.JobRepository
import io.github.leogallego.ansiblejane.model.JobStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ListJobsLocalTool(
    private val repository: JobRepository
) : AapLocalTool<ListJobsLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_jobs",
    description = "List recent jobs with optional status filter"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Filter by status: pending, waiting, running, successful, failed, error, canceled")
        val status: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 10, max 20)")
        @SerialName("page_size")
        val pageSize: Int = 10,
    )

    override suspend fun execute(args: Args): String {
        val statusFilter = args.status?.let { statusStr ->
            JobStatus.entries.filter { it.apiValue == statusStr }.toSet()
        } ?: emptySet()
        val pageSize = args.pageSize.coerceIn(1, 20)
        val result = repository.getRecentJobs(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize,
            statusFilters = statusFilter
        ).getOrThrow()
        return listToolJson("jobs", result.totalCount, result.jobs)
    }
}
