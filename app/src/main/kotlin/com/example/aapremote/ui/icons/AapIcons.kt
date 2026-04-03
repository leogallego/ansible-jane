package com.example.aapremote.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Dns

object AapIcons {

    object Status {
        val New = Icons.Default.HourglassEmpty
        val Pending = Icons.Default.HourglassEmpty
        val Waiting = Icons.Default.HourglassEmpty
        val Running = Icons.Default.PlayCircle
        val Successful = Icons.Default.CheckCircle
        val Failed = Icons.Default.Error
        val Error = Icons.Default.Error
        val Canceled = Icons.Default.Cancel
    }

    object Error {
        val Network = Icons.Default.WifiOff
        val Auth = Icons.Default.Lock
        val Server = Icons.Outlined.Dns
        val Ssl = Icons.Default.GppBad
        val Unknown = Icons.Default.ErrorOutline
    }

    object Navigation {
        val Settings = Icons.Default.Settings
        val Notifications = Icons.Default.Notifications
        val Back = Icons.AutoMirrored.Filled.ArrowBack
    }

    object Action {
        val Launch = Icons.Default.PlayArrow
        val Retry = Icons.Default.Refresh
        val ExpandMore = Icons.Default.ExpandMore
        val ExpandLess = Icons.Default.ExpandLess
    }
}
