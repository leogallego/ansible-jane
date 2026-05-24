package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.ProjectRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ListProjectsLocalTool(
    private val repository: ProjectRepository
) : AapLocalTool<ListProjectsLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_projects",
    description = "List projects with SCM type, URL, branch, and sync status"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Search term to filter projects by name")
        val search: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 25, max 25)")
        @SerialName("page_size")
        val pageSize: Int = 25
    )

    override suspend fun execute(args: Args): String {
        val pageSize = args.pageSize.coerceIn(1, 25)
        val result = repository.getProjects(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize,
            search = args.search
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "projects" to networkJson.encodeToString(result.projects)
        ))
    }
}
