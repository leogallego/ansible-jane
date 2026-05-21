package com.example.aapremote.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.model.Label
import com.example.aapremote.ui.components.LabelChips
import com.example.aapremote.ui.theme.AapRemoteTheme

private val sampleLabels = listOf(
    Label(id = 1, name = "production"),
    Label(id = 2, name = "staging"),
    Label(id = 3, name = "development"),
    Label(id = 4, name = "critical")
)

@PreviewTest
@Preview(showBackground = true, name = "Label Chips None Selected - Light")
@Composable
fun LabelChipsNoneLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        LabelChips(
            labels = sampleLabels,
            selectedLabel = null,
            onLabelSelected = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Label Chips None Selected - Dark")
@Composable
fun LabelChipsNoneDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        LabelChips(
            labels = sampleLabels,
            selectedLabel = null,
            onLabelSelected = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Label Chips One Selected")
@Composable
fun LabelChipsOneSelected() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        LabelChips(
            labels = sampleLabels,
            selectedLabel = sampleLabels[0],
            onLabelSelected = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Label Chips - Large Font")
@Composable
fun LabelChipsLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        LabelChips(
            labels = sampleLabels,
            selectedLabel = null,
            onLabelSelected = {}
        )
    }
}
