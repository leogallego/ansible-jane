package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.CredentialRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class ListCredentialsLocalTool(
    private val repository: CredentialRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_credentials",
        description = "List credentials with optional search (no secrets exposed, only metadata)",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter credentials by name"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 25, max 25)"),
        )
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult = executeSafely {
        val pageSize = (args["page_size"] as? Number)?.toInt()?.coerceIn(1, 25) ?: 25
        val result = repository.getCredentials(
            page = (args["page"] as? Number)?.toInt() ?: 1,
            pageSize = pageSize,
            search = args["search"] as? String
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "credentials" to networkJson.encodeToString(result.credentials)
            ))
        )
    }
}
