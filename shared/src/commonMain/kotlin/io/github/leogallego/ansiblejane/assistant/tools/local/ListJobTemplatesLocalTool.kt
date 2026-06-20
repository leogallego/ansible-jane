package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.listToolJson
import io.github.leogallego.ansiblejane.data.TemplateRepository
import kotlinx.serialization.Serializable

class ListJobTemplatesLocalTool(
    private val repository: TemplateRepository
) : AapLocalTool<ListJobTemplatesLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_job_templates",
    description = "List job templates with optional search and label filter"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Search term to filter templates by name")
        val search: String? = null,
        @property:LLMDescription("Filter by label name (case-insensitive contains)")
        val labels: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
    )

    override suspend fun execute(args: Args): String {
        val result = repository.getTemplates(
            page = args.page.coerceAtLeast(1),
            search = args.search,
            labelFilter = args.labels
        ).getOrThrow()
        return listToolJson("templates", result.totalCount, result.templates)
    }
}
