package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.CredentialRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class GetCredentialLocalTool(
    private val repository: CredentialRepository
) : AapLocalTool<GetCredentialLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    "get_credential", "Get details of a specific credential by ID (type, organization — no secrets exposed)"
) {
    @Serializable
    data class Args(
        @SerialName("credential_id")
        @property:LLMDescription("ID of the credential")
        val credentialId: Int
    )

    override suspend fun execute(args: Args): String {
        val credential = repository.getCredential(args.credentialId).getOrThrow()
        return networkJson.encodeToString(credential)
    }
}
