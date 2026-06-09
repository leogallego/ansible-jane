package io.github.leogallego.ansiblejane.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.ui.graphics.vector.ImageVector

val AppError.icon: ImageVector
    get() = when (this) {
        is AppError.Network -> Icons.Default.WifiOff
        is AppError.Auth -> Icons.Default.Lock
        is AppError.Server -> Icons.Outlined.Dns
        is AppError.Ssl -> Icons.Default.GppBad
        is AppError.Unknown -> Icons.Default.ErrorOutline
    }
