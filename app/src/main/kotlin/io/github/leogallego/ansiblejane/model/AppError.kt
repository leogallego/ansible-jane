package io.github.leogallego.ansiblejane.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.ui.graphics.vector.ImageVector
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

data class ErrorDetail(
    val statusCode: Int? = null,
    val url: String? = null,
    val rawMessage: String? = null
)

sealed class AppError(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val detail: ErrorDetail? = null
) {
    class Network(
        message: String = "Unable to reach the server. Check your connection.",
        detail: ErrorDetail? = null
    ) : AppError(
        title = "Network Error",
        message = message,
        icon = Icons.Default.WifiOff,
        detail = detail
    )

    class Auth(
        message: String = "Authentication failed. Please log in again.",
        detail: ErrorDetail? = null
    ) : AppError(
        title = "Auth Error",
        message = message,
        icon = Icons.Default.Lock,
        detail = detail
    )

    class Server(
        val statusCode: Int,
        message: String = "The server encountered an error.",
        detail: ErrorDetail? = null
    ) : AppError(
        title = "Server Error",
        message = message,
        icon = Icons.Outlined.Dns,
        detail = detail
    )

    class Ssl(
        message: String = "SSL certificate verification failed.",
        detail: ErrorDetail? = null
    ) : AppError(
        title = "SSL Error",
        message = message,
        icon = Icons.Default.GppBad,
        detail = detail
    )

    class Unknown(
        message: String = "An unexpected error occurred.",
        detail: ErrorDetail? = null
    ) : AppError(
        title = "Error",
        message = message,
        icon = Icons.Default.ErrorOutline,
        detail = detail
    )

    companion object {
        fun from(throwable: Throwable): AppError {
            val rawMessage = throwable.message
            return when {
                throwable is SSLHandshakeException || throwable is SSLException -> Ssl(
                    detail = ErrorDetail(rawMessage = rawMessage)
                )
                throwable is UnknownHostException ||
                    throwable is ConnectException ||
                    throwable is SocketTimeoutException -> Network(
                    detail = ErrorDetail(rawMessage = rawMessage)
                )
                throwable is IOException -> Network(
                    detail = ErrorDetail(rawMessage = rawMessage)
                )
                throwable is HttpException -> {
                    val code = throwable.code()
                    val url = throwable.response()?.raw()?.request?.url?.toString()
                    val detail = ErrorDetail(
                        statusCode = code,
                        url = url,
                        rawMessage = rawMessage
                    )
                    when {
                        code == 401 || code == 403 -> Auth(detail = detail)
                        code in 500..599 -> Server(
                            statusCode = code,
                            detail = detail
                        )
                        else -> Server(
                            statusCode = code,
                            message = "Request failed with status $code.",
                            detail = detail
                        )
                    }
                }
                else -> Unknown(
                    message = rawMessage ?: "An unexpected error occurred.",
                    detail = ErrorDetail(rawMessage = rawMessage)
                )
            }
        }
    }
}
