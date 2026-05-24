package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.HostRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetHostFactsLocalTool(
    private val repository: HostRepository
) : AapLocalTool<GetHostFactsLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_host_facts", "Get Ansible facts for a specific host by ID"
) {
    @Serializable
    data class Args(
        @SerialName("host_id")
        @property:LLMDescription("ID of the host")
        val hostId: Int
    )

    override suspend fun execute(args: Args): String {
        val facts = repository.getHostFacts(args.hostId).getOrThrow()
        return networkJson.encodeToString(facts)
    }
}
