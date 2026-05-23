package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.InventoryRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListInventoriesLocalTool(
    private val repository: InventoryRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_inventories",
        description = "List inventories with optional search filter",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter inventories by name"),
            Triple("page", "integer", "Page number (default 1)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val result = repository.getInventories(
            page = args.pageArg(),
            search = args.stringArg("search")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "inventories" to networkJson.encodeToString(result.inventories)
            ))
        )
    }
}
