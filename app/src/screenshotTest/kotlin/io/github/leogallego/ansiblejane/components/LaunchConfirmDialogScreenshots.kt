package io.github.leogallego.ansiblejane.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.ui.components.LaunchConfirmDialog
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@PreviewTest
@Preview(showBackground = true, name = "Launch Dialog - Light")
@Composable
fun LaunchConfirmDialogLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        LaunchConfirmDialog(
            templateName = "Deploy Production App",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Launch Dialog - Dark")
@Composable
fun LaunchConfirmDialogDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        LaunchConfirmDialog(
            templateName = "Deploy Production App",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Launch Dialog - Large Font")
@Composable
fun LaunchConfirmDialogLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        LaunchConfirmDialog(
            templateName = "Deploy Production App",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
