package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.InfrastructureRepository

class GetMeshTopologyLocalTool(
    private val repository: InfrastructureRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_mesh_topology",
        description = "Get the automation mesh topology — nodes, connections, and link status",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult = executeSafely {
        val topology = repository.getMeshTopology().getOrThrow()
        ToolResult(success = true, data = topology.toString())
    }
}
