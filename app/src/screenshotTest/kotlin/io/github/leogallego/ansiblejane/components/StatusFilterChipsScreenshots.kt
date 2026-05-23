package io.github.leogallego.ansiblejane.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.ui.components.StatusFilterChips
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@PreviewTest
@Preview(showBackground = true, name = "Filter Chips None Selected - Light")
@Composable
fun StatusFilterChipsNoneLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        StatusFilterChips(
            activeFilters = setOf(JobStatus.SUCCESSFUL),
            onToggleFilter = {}
        )
    }
}
