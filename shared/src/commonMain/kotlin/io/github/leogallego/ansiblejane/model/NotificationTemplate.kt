package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationTemplate(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("notification_type") val notificationType: String = "",
    val organization: Int? = null
)
