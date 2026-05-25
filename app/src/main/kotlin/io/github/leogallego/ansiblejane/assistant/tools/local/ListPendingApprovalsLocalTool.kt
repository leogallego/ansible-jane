package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ListPendingApprovalsLocalTool(
    private val repository: IWorkflowRepository
) : AapLocalTool<ListPendingApprovalsLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "list_pending_approvals",
    description = "List pending workflow approval requests"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
        @property:LLMDescription("Results per page (default 25)")
        @SerialName("page_size")
        val pageSize: Int = 25
    )

    override suspend fun execute(args: Args): String {
        val result = repository.getPendingApprovals(
            page = args.page.coerceAtLeast(1),
            pageSize = args.pageSize.coerceIn(1, 100)
        ).getOrThrow()
        return networkJson.encodeToString(mapOf(
            "count" to result.totalCount.toString(),
            "has_more" to result.hasMore.toString(),
            "results" to networkJson.encodeToString(result.approvals)
        ))
    }
}
