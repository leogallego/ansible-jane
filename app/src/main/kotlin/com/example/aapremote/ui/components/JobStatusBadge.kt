package com.example.aapremote.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aapremote.model.JobStatus
import com.example.aapremote.ui.icons.AapIcons
import com.example.aapremote.ui.theme.AapRemoteTheme

@Composable
fun JobStatusBadge(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, label) = statusConfig(status)

    val pulseScale = if (status == JobStatus.RUNNING) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        scale
    } else {
        1f
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
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

@Composable
private fun statusConfig(status: JobStatus): StatusConfig {
    val colors = AapRemoteTheme.statusColors
    return when (status) {
        JobStatus.NEW -> StatusConfig(colors.new, AapIcons.Status.New, "New")
        JobStatus.PENDING -> StatusConfig(colors.pending, AapIcons.Status.Pending, "Pending")
        JobStatus.WAITING -> StatusConfig(colors.waiting, AapIcons.Status.Waiting, "Waiting")
        JobStatus.RUNNING -> StatusConfig(colors.running, AapIcons.Status.Running, "Running")
        JobStatus.SUCCESSFUL -> StatusConfig(colors.successful, AapIcons.Status.Successful, "Successful")
        JobStatus.FAILED -> StatusConfig(colors.failed, AapIcons.Status.Failed, "Failed")
        JobStatus.ERROR -> StatusConfig(colors.error, AapIcons.Status.Error, "Error")
        JobStatus.CANCELED -> StatusConfig(colors.canceled, AapIcons.Status.Canceled, "Canceled")
    }
}
