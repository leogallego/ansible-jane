package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.CredentialRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

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
    override suspend fun execute(args: Map<String, Any>): ToolResult = executeSafely {
        val credentialId = (args["credential_id"] as? Number)?.toInt()
            ?: return@executeSafely ToolResult(success = false, data = "credential_id is required", errorType = ErrorType.NOT_FOUND)
        val credential = repository.getCredential(credentialId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(credential))
    }
}
