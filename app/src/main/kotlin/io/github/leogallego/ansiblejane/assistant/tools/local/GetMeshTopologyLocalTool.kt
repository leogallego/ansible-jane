package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import kotlinx.serialization.json.JsonObject

class GetMeshTopologyLocalTool(
    private val repository: InfrastructureRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_mesh_topology",
        description = "Get the automation mesh topology — nodes, connections, and link status",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val topology = repository.getMeshTopology().getOrThrow()
        ToolResult(success = true, data = topology.toString())
    }
}
