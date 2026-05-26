package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.ErrorDetail
import io.github.leogallego.ansiblejane.ui.icons.AapIcons
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@Composable
fun ErrorMessage(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = error.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = error.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (error.detail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.clickable { showDetails = !showDetails },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val toggleIcon = if (showDetails) AapIcons.Action.ExpandLess
                        else AapIcons.Action.ExpandMore
                    Icon(
                        imageVector = toggleIcon,
                        contentDescription = stringResource(if (showDetails) R.string.hide_details else R.string.show_details),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(if (showDetails) R.string.hide_details else R.string.show_details),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showDetails) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            error.detail.statusCode?.let {
                                Text(
                                    text = "Status: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            error.detail.url?.let {
                                Text(
                                    text = "URL: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            error.detail.rawMessage?.let {
                                Text(
                                    text = "Detail: $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Error - Network with Retry")
@Composable
private fun ErrorMessageNetworkPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        ErrorMessage(error = AppError.Network(), onRetry = {})
    }
}

@Preview(showBackground = true, name = "Error - Server with Details")
@Composable
private fun ErrorMessageWithDetailsPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        ErrorMessage(
            error = AppError.Server(
                statusCode = 500,
                detail = ErrorDetail(
                    statusCode = 500,
                    url = "https://aap.example.com/api/v2/jobs/",
                    rawMessage = "Internal Server Error"
                )
            ),
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Error - Auth without Retry")
@Composable
private fun ErrorMessageNoRetryPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        ErrorMessage(error = AppError.Auth())
    }
}
