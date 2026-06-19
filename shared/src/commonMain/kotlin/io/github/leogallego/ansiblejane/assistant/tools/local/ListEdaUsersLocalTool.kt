package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.EdaReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListEdaUsersLocalTool(
    private val repository: EdaReadOnlyRepository
) : AapLocalTool<ListEdaUsersLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_eda_users",
    description = "List EDA users (requires EDA to be installed on the AAP instance)"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 20, max 20)")
        @SerialName("page_size")
        val pageSize: Int = 20,
    )

    override suspend fun execute(args: Args): String {
        val pageSize = args.pageSize.coerceIn(1, 20)
        val result = repository.getUsers(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize
        ).getOrThrow()
        return buildJsonObject {
            put("count", result.totalCount)
            put("users", networkJson.encodeToJsonElement(result.items))
        }.toString()
    }
}
