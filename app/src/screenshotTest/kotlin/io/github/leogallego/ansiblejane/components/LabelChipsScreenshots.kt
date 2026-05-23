package io.github.leogallego.ansiblejane.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.ui.components.LabelChips
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        LabelChips(
            labels = sampleLabels,
            selectedLabel = null,
            onLabelSelected = {}
        )
    }
}
