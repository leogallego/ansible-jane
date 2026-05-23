package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.CredentialRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetCredentialLocalTool(
    private val repository: CredentialRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_credential",
        description = "Get details of a specific credential by ID (type, organization — no secrets exposed)",
        parametersSchema = buildToolSchema(
            Triple("credential_id", "integer", "ID of the credential"),
            required = listOf("credential_id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val credentialId = args.intArg("credential_id")
            ?: return@executeSafely ToolResult(success = false, data = "credential_id is required", errorType = ErrorType.NOT_FOUND)
        val credential = repository.getCredential(credentialId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(credential))
    }
}
