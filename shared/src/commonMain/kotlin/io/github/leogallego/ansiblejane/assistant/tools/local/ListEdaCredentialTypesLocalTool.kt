package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.EdaReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ListEdaCredentialTypesLocalTool(
    private val repository: EdaReadOnlyRepository
) : AapLocalTool<ListEdaCredentialTypesLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_eda_credential_types",
    description = "List EDA credential types available for rulebook activations"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Filter by name")
        val name: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 20, max 20)")
        @SerialName("page_size")
        val pageSize: Int = 20,
    )

    override suspend fun execute(args: Args): String {
        val pageSize = args.pageSize.coerceIn(1, 20)
        val result = repository.getCredentialTypes(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize,
            name = args.name
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "credential_types" to networkJson.encodeToString(result.items)
        ))
    }
}
