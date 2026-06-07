package io.github.leogallego.ansiblejane.platform

import java.awt.Desktop
import java.net.URI
import java.util.Properties

actual class PlatformUtils {
    actual fun openUrl(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    actual fun showToast(message: String) {
        println("Toast: $message")
    }

    actual fun getAppVersion(): String {
        val props = Properties()
        val stream = PlatformUtils::class.java.getResourceAsStream("/version.properties")
        return if (stream != null) {
            props.load(stream)
            props.getProperty("version", "unknown")
        } else {
            "unknown"
        }
    }
}
