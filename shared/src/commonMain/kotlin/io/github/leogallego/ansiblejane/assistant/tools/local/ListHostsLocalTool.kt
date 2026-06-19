package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.HostRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListHostsLocalTool(
    private val repository: HostRepository
) : AapLocalTool<ListHostsLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_hosts",
    description = "List hosts, optionally filtered by inventory ID or search term"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Filter hosts by inventory ID")
        @SerialName("inventory_id")
        val inventoryId: Int? = null,
        @property:LLMDescription("Search term to filter hosts by name")
        val search: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
    )

    override suspend fun execute(args: Args): String {
        val page = args.page.coerceAtLeast(1)
        val search = args.search

        val result = if (args.inventoryId != null) {
            repository.getInventoryHosts(
                inventoryId = args.inventoryId,
                page = page,
                search = search
            )
        } else {
            repository.getAllHosts(
                page = page,
                search = search
            )
        }.getOrThrow()

        return buildJsonObject {
            put("count", result.totalCount)
            put("hosts", networkJson.encodeToJsonElement(result.hosts))
        }.toString()
    }
}
