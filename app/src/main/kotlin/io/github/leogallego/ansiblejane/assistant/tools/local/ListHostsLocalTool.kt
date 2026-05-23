package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.HostRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val inventoryId = args.intArg("inventory_id")
        val page = args.pageArg()
        val search = args.stringArg("search")

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
    }
}
