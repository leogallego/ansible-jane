package io.github.leogallego.ansiblejane.ui.components

enum class TimeFormat {
    SYSTEM,
    HOURS_24,
    HOURS_12;

    val displayName: String
        get() = when (this) {
            SYSTEM -> "System"
            HOURS_24 -> "24-hour"
            HOURS_12 -> "12-hour"
        }
}
