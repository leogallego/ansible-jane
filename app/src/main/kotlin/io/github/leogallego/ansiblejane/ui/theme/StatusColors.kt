package io.github.leogallego.ansiblejane.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class StatusColors(
    val successful: Color = Color(0xFF4CAF50),
    val successfulDim: Color = Color(0xFF2E7D32),
    val running: Color = Color(0xFFFF9800),
    val healthDegraded: Color = Color(0xFFE6A817),
)

val LocalStatusColors = staticCompositionLocalOf { StatusColors() }
