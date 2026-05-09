package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.HostRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class ListHostsLocalTool(
    private val repository: HostRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_hosts",
        description = "List hosts, optionally filtered by inventory ID or search term",
        parametersSchema = buildToolSchema(
            Triple("inventory_id", "integer", "Filter hosts by inventory ID"),
            Triple("search", "string", "Search term to filter hosts by name"),
            Triple("page", "integer", "Page number (default 1)"),
        )
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val inventoryId = (args["inventory_id"] as? Number)?.toInt()
            val page = (args["page"] as? Number)?.toInt() ?: 1
            val search = args["search"] as? String

            val result = if (inventoryId != null) {
                repository.getInventoryHosts(
                    inventoryId = inventoryId,
                    page = page,
                    search = search
                )
            } else {
                repository.getAllHosts(
                    page = page,
                    search = search
                )
            }.getOrThrow()

            ToolResult(
                success = true,
                data = networkJson.encodeToString(mapOf(
                    "count" to result.totalCount.toString(),
                    "hosts" to networkJson.encodeToString(result.hosts)
                ))
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}
