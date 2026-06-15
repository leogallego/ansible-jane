package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.EdaActivationRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetEdaActivationLocalTool(
    private val repository: EdaActivationRepository
) : AapLocalTool<GetEdaActivationLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_eda_activation", "Get details of a specific EDA rulebook activation by ID — status, restart policy, decision environment"
) {
    @Serializable
    data class Args(
        @SerialName("activation_id")
        @property:LLMDescription("ID of the EDA activation")
        val activationId: Int
    )

    override suspend fun execute(args: Args): String {
        val activation = repository.getActivation(args.activationId).getOrThrow()
        return networkJson.encodeToString(activation)
    }
}
