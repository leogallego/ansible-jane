package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable

enum class PlatformType {
    AAP,
    AWX,
    JEWEL,
    UNKNOWN
}

enum class AapComponent {
    CONTROLLER,
    EDA,
    HUB,
    GATEWAY
}

@Serializable
data class InstanceInfo(
    val controllerVersion: String = "",
    val gatewayVersion: String = "",
    val edaVersion: String = "",
    val platformType: String = "UNKNOWN",
    val aapVersion: String? = null,
    val components: List<String> = emptyList()
) {
    val resolvedPlatformType: PlatformType
        get() = try {
            PlatformType.valueOf(platformType)
        } catch (_: Exception) {
            PlatformType.UNKNOWN
        }

    val resolvedComponents: Set<AapComponent>
        get() = components.mapNotNull {
            try {
                AapComponent.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }.toSet()

    fun hasComponent(component: AapComponent): Boolean =
        component.name in components
}
