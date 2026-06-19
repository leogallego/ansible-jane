package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListInstancesLocalTool(
    private val repository: InfrastructureRepository
) : AapLocalTool<ListInstancesLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_instances",
    description = "List AAP cluster instances with node type, capacity, and health status"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 25, max 25)")
        @SerialName("page_size")
        val pageSize: Int = 25
    )

    override suspend fun execute(args: Args): String {
        val pageSize = args.pageSize.coerceIn(1, 25)
        val result = repository.getInstances(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize
        ).getOrThrow()
        return buildJsonObject {
            put("count", result.totalCount)
            put("instances", networkJson.encodeToJsonElement(result.instances))
        }.toString()
    }
}
