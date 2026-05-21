package com.example.aapremote.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.model.JobStatus
import com.example.aapremote.ui.components.StatusFilterChips
import com.example.aapremote.ui.theme.AapRemoteTheme

@PreviewTest
@Preview(showBackground = true, name = "Filter Chips None Selected - Light")
@Composable
fun StatusFilterChipsNoneLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        StatusFilterChips(
            activeFilters = emptySet(),
            onToggleFilter = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Filter Chips None Selected - Dark")
@Composable
fun StatusFilterChipsNoneDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        StatusFilterChips(
            activeFilters = emptySet(),
            onToggleFilter = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Filter Chips Some Selected")
@Composable
fun StatusFilterChipsSomeSelected() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        StatusFilterChips(
            activeFilters = setOf(JobStatus.RUNNING, JobStatus.FAILED),
            onToggleFilter = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Filter Chips - Large Font")
@Composable
fun StatusFilterChipsLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        StatusFilterChips(
            activeFilters = setOf(JobStatus.SUCCESSFUL),
            onToggleFilter = {}
        )
    }
}
