package io.github.leogallego.ansiblejane.platform

import java.awt.Desktop
import java.net.URI

actual class PlatformUtils {
    actual fun openUrl(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    actual fun showToast(message: String) {
        println("Toast: $message")
    }

    actual fun getAppVersion(): String = "0.7.6-alpha.0"
}
