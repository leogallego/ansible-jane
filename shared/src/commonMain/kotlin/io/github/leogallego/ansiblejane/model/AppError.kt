package io.github.leogallego.ansiblejane.model

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException

data class ErrorDetail(
    val statusCode: Int? = null,
    val url: String? = null,
    val rawMessage: String? = null
)

sealed class AppError(
    val title: String,
    val message: String,
    val detail: ErrorDetail? = null
) {
    class Network(
        message: String = "Unable to reach the server. Check your connection.",
        detail: ErrorDetail? = null
    ) : AppError(title = "Network Error", message = message, detail = detail)

    class Auth(
        message: String = "Authentication failed. Please log in again.",
        detail: ErrorDetail? = null
    ) : AppError(title = "Auth Error", message = message, detail = detail)

    class Server(
        val statusCode: Int,
        message: String = "The server encountered an error.",
        detail: ErrorDetail? = null
    ) : AppError(title = "Server Error", message = message, detail = detail)

    class Ssl(
        message: String = "SSL certificate verification failed.",
        detail: ErrorDetail? = null
    ) : AppError(title = "SSL Error", message = message, detail = detail)

    class Unknown(
        message: String = "An unexpected error occurred.",
        detail: ErrorDetail? = null
    ) : AppError(title = "Error", message = message, detail = detail)

    companion object {
        fun from(throwable: Throwable): AppError {
            val rawMessage = throwable.message
            return when (throwable) {
                is ClientRequestException -> {
                    val code = throwable.response.status.value
                    val detail = ErrorDetail(statusCode = code, rawMessage = rawMessage)
                    when {
                        code == 401 || code == 403 -> Auth(detail = detail)
                        else -> Server(statusCode = code, detail = detail)
                    }
                }
                is ServerResponseException -> {
                    val code = throwable.response.status.value
                    Server(statusCode = code, detail = ErrorDetail(statusCode = code, rawMessage = rawMessage))
                }
                is ResponseException -> {
                    val code = throwable.response.status.value
                    Server(
                        statusCode = code,
                        message = "Request failed with status $code.",
                        detail = ErrorDetail(statusCode = code, rawMessage = rawMessage)
                    )
                }
                is HttpRequestTimeoutException -> Network(
                    message = "Request timed out.",
                    detail = ErrorDetail(rawMessage = rawMessage)
                )
                else -> classifyByName(throwable)
            }
        }

        private fun classifyByName(throwable: Throwable): AppError {
            val msg = throwable.message ?: ""
            if (isSslRelated(throwable)) return Ssl(detail = ErrorDetail(rawMessage = msg))
            if (isNetworkRelated(throwable)) return Network(detail = ErrorDetail(rawMessage = msg))
            return Unknown(
                message = msg.ifBlank { "An unexpected error occurred." },
                detail = ErrorDetail(rawMessage = msg)
            )
        }

        private fun isSslRelated(throwable: Throwable): Boolean {
            var current: Throwable? = throwable
            while (current != null) {
                val name = current::class.simpleName ?: ""
                if ("SSL" in name || "Tls" in name) return true
                current = current.cause
            }
            return false
        }

        private fun isNetworkRelated(throwable: Throwable): Boolean {
            var current: Throwable? = throwable
            while (current != null) {
                val name = current::class.simpleName ?: ""
                if ("Timeout" in name || "Connect" in name ||
                    "UnknownHost" in name || "IOException" in name ||
                    "SocketException" in name || "NoRouteToHost" in name
                ) return true
                current = current.cause
            }
            return false
        }
    }
}
