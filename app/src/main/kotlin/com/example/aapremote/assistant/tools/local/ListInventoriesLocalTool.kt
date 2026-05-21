package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.InventoryRepository
import com.example.aapremote.network.networkJson
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
            page = args.intArg("page") ?: 1,
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
