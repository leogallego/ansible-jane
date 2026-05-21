package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.HostRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetHostFactsLocalTool(
    private val repository: HostRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_host_facts",
        description = "Get Ansible facts for a specific host by ID",
        parametersSchema = buildToolSchema(
            Triple("host_id", "integer", "ID of the host"),
            required = listOf("host_id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val hostId = args.intArg("host_id")
            ?: return@executeSafely ToolResult(success = false, data = "host_id is required", errorType = ErrorType.NOT_FOUND)
        val facts = repository.getHostFacts(hostId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(facts))
    }
}
