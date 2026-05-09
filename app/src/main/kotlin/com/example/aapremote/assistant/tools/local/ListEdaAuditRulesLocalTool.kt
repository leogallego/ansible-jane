package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.EdaAuditRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

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
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val pageSize = (args["page_size"] as? Number)?.toInt()?.coerceIn(1, 20) ?: 10
            val result = repository.getAuditRules(
                page = (args["page"] as? Number)?.toInt() ?: 1,
                pageSize = pageSize
            ).getOrThrow()
            ToolResult(
                success = true,
                data = networkJson.encodeToString(mapOf(
                    "count" to result.totalCount.toString(),
                    "audit_rules" to networkJson.encodeToString(result.auditRules)
                ))
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}
