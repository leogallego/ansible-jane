package io.github.leogallego.ansiblejane.platform

expect class PlatformUtils {
    fun openUrl(url: String)
    fun showToast(message: String)
    fun getAppVersion(): String
}
