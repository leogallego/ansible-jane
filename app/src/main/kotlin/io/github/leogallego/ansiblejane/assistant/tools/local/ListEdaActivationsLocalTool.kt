package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.EdaActivationRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListEdaActivationsLocalTool(
    private val repository: EdaActivationRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_eda_activations",
        description = "List EDA rulebook activations with status, restart policy, and rulebook info",
        parametersSchema = buildToolSchema(
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 20, max 20)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 20) ?: 20
        val result = repository.getActivations(
            page = args.pageArg(),
            pageSize = pageSize
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "activations" to networkJson.encodeToString(result.activations)
            ))
        )
    }
}
