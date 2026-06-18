package io.github.leogallego.ansiblejane.assistant.engine

import android.util.Log

actual object DebugLog {
    actual var enabled: Boolean = false
    actual fun d(tag: String, msg: String) {
        if (enabled) Log.d(tag, msg)
    }
    actual fun w(tag: String, msg: String) {
        if (enabled) Log.w(tag, msg)
    }
    actual fun e(tag: String, msg: String, t: Throwable?) {
        if (enabled) {
            if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg)
        }
    }
}
