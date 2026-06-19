package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.HubRepository
import io.github.leogallego.ansiblejane.data.TokenManager
import io.github.leogallego.ansiblejane.model.AapComponent
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ListHubEeRegistriesLocalTool(
    private val repository: HubRepository,
    private val tokenManager: TokenManager
) : AapLocalTool<ListHubEeRegistriesLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_hub_ee_registries",
    description = "List remote container registries configured in Private Automation Hub"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Search term to filter registries by name")
        val search: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 20, max 20)")
        @SerialName("page_size")
        val pageSize: Int = 20
    )

    override suspend fun execute(args: Args): String {
        val info = tokenManager.activeInstance.value?.instanceInfo
        if (info != null && !info.hasComponent(AapComponent.HUB)) {
            return """{"error": "Hub is not available on this instance"}"""
        }
        val result = repository.getEeRegistries(
            page = args.page.coerceAtLeast(1),
            pageSize = args.pageSize.coerceIn(1, 20),
            search = args.search
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "ee_registries" to networkJson.encodeToString(result.items)
        ))
    }
}
