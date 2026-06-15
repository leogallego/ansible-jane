package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ListTokensLocalTool(
    private val repository: ControllerReadOnlyRepository
) : AapLocalTool<ListTokensLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_tokens",
    description = "List personal access tokens (metadata only, no secrets)"
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
        val result = repository.getTokens(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "tokens" to networkJson.encodeToString(result.items)
        ))
    }
}
