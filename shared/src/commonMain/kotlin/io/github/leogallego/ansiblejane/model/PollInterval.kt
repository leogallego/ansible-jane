package io.github.leogallego.ansiblejane.model

enum class PollInterval(val minutes: Int, val displayName: String) {
    MINUTES_15(15, "15 minutes"),
    MINUTES_30(30, "30 minutes"),
    MINUTES_60(60, "1 hour")
}
