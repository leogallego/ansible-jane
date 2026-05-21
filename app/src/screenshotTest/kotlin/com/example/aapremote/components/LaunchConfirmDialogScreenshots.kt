package com.example.aapremote.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.ui.components.LaunchConfirmDialog
import com.example.aapremote.ui.theme.AapRemoteTheme

@PreviewTest
@Preview(showBackground = true, name = "Launch Dialog - Light")
@Composable
fun LaunchConfirmDialogLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
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
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
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
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        LaunchConfirmDialog(
            templateName = "Deploy Production App",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
