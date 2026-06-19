package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListNotificationTemplatesLocalTool(
    private val repository: ControllerReadOnlyRepository
) : AapLocalTool<ListNotificationTemplatesLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_notification_templates",
    description = "List notification templates (Slack, email, webhook, etc.)"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Search term to filter by name")
        val search: String? = null,
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 25, max 25)")
        @SerialName("page_size")
        val pageSize: Int = 25
    )

    override suspend fun execute(args: Args): String {
        val pageSize = args.pageSize.coerceIn(1, 25)
        val result = repository.getNotificationTemplates(
            page = args.page.coerceAtLeast(1),
            pageSize = pageSize,
            search = args.search
        ).getOrThrow()
        return buildJsonObject {
            put("count", result.totalCount)
            put("notification_templates", networkJson.encodeToJsonElement(result.items))
        }.toString()
    }
}
