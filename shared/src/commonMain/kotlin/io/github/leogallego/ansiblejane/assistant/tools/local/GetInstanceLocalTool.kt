package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetInstanceLocalTool(
    private val repository: InfrastructureRepository
) : AapLocalTool<GetInstanceLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_instance", "Get details of a specific AAP instance by ID, including capacity and health"
) {
    @Serializable
    data class Args(
        @SerialName("instance_id")
        @property:LLMDescription("ID of the instance")
        val instanceId: Int
    )

    override suspend fun execute(args: Args): String {
        val instance = repository.getInstance(args.instanceId).getOrThrow()
        return networkJson.encodeToString(instance)
    }
}
