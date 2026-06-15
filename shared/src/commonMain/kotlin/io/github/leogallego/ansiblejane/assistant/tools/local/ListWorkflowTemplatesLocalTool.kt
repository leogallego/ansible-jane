package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.WorkflowRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ListWorkflowTemplatesLocalTool(
    private val repository: WorkflowRepository
) : AapLocalTool<ListWorkflowTemplatesLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_workflow_templates",
    description = "List workflow job templates with optional search and label filter"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Search term to filter by name")
        val search: String? = null,
        @property:LLMDescription("Filter by label name (case-insensitive contains)")
        val labels: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
    )

    override suspend fun execute(args: Args): String {
        val result = repository.getWorkflowTemplates(
            page = args.page.coerceAtLeast(1),
            search = args.search,
            labelFilter = args.labels
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "templates" to networkJson.encodeToString(result.templates)
        ))
    }
}
