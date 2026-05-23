package io.github.leogallego.ansiblejane.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class StatusColors(
    val successful: Color = Color(0xFF4CAF50),
    val failed: Color = Color(0xFFF44336),
    val error: Color = Color(0xFFD32F2F),
    val running: Color = Color(0xFFFF9800),
    val pending: Color = Color(0xFF9E9E9E),
    val waiting: Color = Color(0xFF9E9E9E),
    val new: Color = Color(0xFF9E9E9E),
    val canceled: Color = Color(0xFF2196F3)
)

val LocalStatusColors = staticCompositionLocalOf { StatusColors() }
