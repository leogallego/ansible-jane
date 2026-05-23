package io.github.leogallego.ansiblejane.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.ui.components.JobStatusBadge
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@PreviewTest
@Preview(showBackground = true, name = "All Statuses - Light")
@Composable
fun JobStatusBadgeAllLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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

@PreviewTest
@Preview(showBackground = true, name = "All Statuses - Dark")
@Composable
fun JobStatusBadgeAllDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
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

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "All Statuses - Large Font")
@Composable
fun JobStatusBadgeAllLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
