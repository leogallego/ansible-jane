package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.EdaReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListEdaRulebooksLocalTool(
    private val repository: EdaReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_eda_rulebooks",
        description = "List EDA rulebooks (requires EDA to be installed on the AAP instance)",
        parametersSchema = buildToolSchema(
            Triple("name", "string", "Filter by rulebook name"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 20, max 20)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 20) ?: 20
        val result = repository.getRulebooks(
            page = args.pageArg(),
            pageSize = pageSize,
            name = args.stringArg("name")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "rulebooks" to networkJson.encodeToString(result.items)
            ))
        )
    }
}
