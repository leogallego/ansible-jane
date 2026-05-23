package io.github.leogallego.ansiblejane.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.ErrorDetail
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@PreviewTest
@Preview(showBackground = true, name = "Network Error - Light")
@Composable
fun ErrorMessageNetworkLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Network(),
            onRetry = {}
        )
    }
}
