package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.CredentialRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 25) ?: 25
        val result = repository.getCredentials(
            page = args.pageArg(),
            pageSize = pageSize,
            search = args.stringArg("search")
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
