package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.EdaAuditRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListEdaAuditRulesLocalTool(
    private val repository: EdaAuditRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_eda_audit_rules",
        description = "List EDA rule audit events showing rulebook activation history",
        parametersSchema = buildToolSchema(
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 10, max 20)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 20) ?: 10
        val result = repository.getAuditRules(
            page = args.pageArg(),
            pageSize = pageSize
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "audit_rules" to networkJson.encodeToString(result.auditRules)
            ))
        )
    }
}
