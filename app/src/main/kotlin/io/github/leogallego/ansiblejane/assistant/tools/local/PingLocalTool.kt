package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.EmptyArgs
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString

class PingLocalTool(
    private val repository: InfrastructureRepository
) : AapLocalTool<EmptyArgs>(
    typeToken<EmptyArgs>(), EmptyArgs.serializer(),
    "ping", "Quick health check of the AAP cluster — returns version, HA status, and node info"
) {
    override suspend fun execute(args: EmptyArgs): String {
        val ping = repository.ping().getOrThrow()
        return networkJson.encodeToString(ping)
    }
}
