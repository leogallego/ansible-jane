package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class PingLocalTool(
    private val repository: InfrastructureRepository
) : LocalTool(
    spec = ToolSpec(
        name = "ping",
        description = "Quick health check of the AAP cluster — returns version, HA status, and node info",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val ping = repository.ping().getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(ping))
    }
}
