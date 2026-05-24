package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.EmptyArgs
import io.github.leogallego.ansiblejane.data.InfrastructureRepository

class GetMeshTopologyLocalTool(
    private val repository: InfrastructureRepository
) : AapLocalTool<EmptyArgs>(
    typeToken<EmptyArgs>(), EmptyArgs.serializer(),
    "get_mesh_topology", "Get the automation mesh topology — nodes, connections, and link status"
) {
    override suspend fun execute(args: EmptyArgs): String {
        val topology = repository.getMeshTopology().getOrThrow()
        return topology.toString()
    }
}
