package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.InventoryRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListInventoriesLocalTool(
    private val repository: InventoryRepository
) : AapLocalTool<ListInventoriesLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_inventories",
    description = "List inventories with optional search filter"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Search term to filter inventories by name")
        val search: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1
    )

    override suspend fun execute(args: Args): String {
        val result = repository.getInventories(
            page = args.page.coerceAtLeast(1),
            search = args.search
        ).getOrThrow()
        return buildJsonObject {
            put("count", result.totalCount)
            put("inventories", networkJson.encodeToJsonElement(result.inventories))
        }.toString()
    }
}
