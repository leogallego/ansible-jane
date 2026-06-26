package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.ui.icons.AapIcons
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

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
    val statusColors = AnsibleJaneTheme.statusColors
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        JobStatus.NEW -> StatusConfig(scheme.outline, AapIcons.Status.New, stringResource(Res.string.job_status_new))
        JobStatus.PENDING -> StatusConfig(scheme.outline, AapIcons.Status.Pending, stringResource(Res.string.job_status_pending))
        JobStatus.WAITING -> StatusConfig(scheme.outline, AapIcons.Status.Waiting, stringResource(Res.string.job_status_waiting))
        JobStatus.RUNNING -> StatusConfig(statusColors.running, AapIcons.Status.Running, stringResource(Res.string.job_status_running))
        JobStatus.SUCCESSFUL -> StatusConfig(statusColors.successful, AapIcons.Status.Successful, stringResource(Res.string.job_status_successful))
        JobStatus.FAILED -> StatusConfig(scheme.error, AapIcons.Status.Failed, stringResource(Res.string.job_status_failed))
        JobStatus.ERROR -> StatusConfig(scheme.error, AapIcons.Status.Error, stringResource(Res.string.job_status_error))
        JobStatus.CANCELED -> StatusConfig(scheme.secondary, AapIcons.Status.Canceled, stringResource(Res.string.job_status_canceled))
    }
}

@PreviewLightDark
@Composable
private fun JobStatusBadgePreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JobStatus.entries.forEach { status ->
                JobStatusBadge(status = status)
            }
        }
    }
}
