package com.example.aapremote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aapremote.model.JobStatus
import com.example.aapremote.ui.theme.*

@Composable
fun JobStatusBadge(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, label) = statusConfig(status)

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private data class StatusConfig(val color: Color, val icon: ImageVector, val label: String)

private fun statusConfig(status: JobStatus): StatusConfig = when (status) {
    JobStatus.NEW -> StatusConfig(StatusNew, Icons.Default.HourglassEmpty, "New")
    JobStatus.PENDING -> StatusConfig(StatusPending, Icons.Default.HourglassEmpty, "Pending")
    JobStatus.WAITING -> StatusConfig(StatusWaiting, Icons.Default.HourglassEmpty, "Waiting")
    JobStatus.RUNNING -> StatusConfig(StatusRunning, Icons.Default.PlayCircle, "Running")
    JobStatus.SUCCESSFUL -> StatusConfig(StatusSuccessful, Icons.Default.CheckCircle, "Successful")
    JobStatus.FAILED -> StatusConfig(StatusFailed, Icons.Default.Error, "Failed")
    JobStatus.ERROR -> StatusConfig(StatusError, Icons.Default.Error, "Error")
    JobStatus.CANCELED -> StatusConfig(StatusCanceled, Icons.Default.Cancel, "Canceled")
}
