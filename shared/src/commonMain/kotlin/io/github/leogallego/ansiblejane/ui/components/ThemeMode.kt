package io.github.leogallego.ansiblejane.ui.components

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    val displayName: String
        get() = when (this) {
            SYSTEM -> "System"
            LIGHT -> "Light"
            DARK -> "Dark"
        }
}
