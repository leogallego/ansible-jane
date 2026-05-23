package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.HostRepository
import io.github.leogallego.ansiblejane.network.networkJson
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
