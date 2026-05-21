package com.example.aapremote.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.model.AppError
import com.example.aapremote.model.ErrorDetail
import com.example.aapremote.ui.components.ErrorMessage
import com.example.aapremote.ui.theme.AapRemoteTheme

@PreviewTest
@Preview(showBackground = true, name = "Network Error - Light")
@Composable
fun ErrorMessageNetworkLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Network(),
            onRetry = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Network Error - Dark")
@Composable
fun ErrorMessageNetworkDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Network(),
            onRetry = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Auth Error")
@Composable
fun ErrorMessageAuth() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Auth(),
            onRetry = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Server Error with Detail")
@Composable
fun ErrorMessageServerWithDetail() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Server(
                statusCode = 500,
                detail = ErrorDetail(
                    statusCode = 500,
                    url = "https://aap.example.com/api/v2/job_templates/",
                    rawMessage = "Internal Server Error"
                )
            ),
            onRetry = {}
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "SSL Error")
@Composable
fun ErrorMessageSsl() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Ssl(),
            onRetry = null
        )
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Network Error - Large Font")
@Composable
fun ErrorMessageNetworkLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Network(),
            onRetry = {}
        )
    }
}
