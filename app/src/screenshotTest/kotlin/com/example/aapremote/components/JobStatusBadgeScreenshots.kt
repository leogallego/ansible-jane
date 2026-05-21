package com.example.aapremote.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.model.JobStatus
import com.example.aapremote.ui.components.JobStatusBadge
import com.example.aapremote.ui.theme.AapRemoteTheme

@PreviewTest
@Preview(showBackground = true, name = "All Statuses - Light")
@Composable
fun JobStatusBadgeAllLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
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
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
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
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
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
