package io.github.leogallego.ansiblejane.assistant.engine

actual object DebugLog {
    actual fun d(tag: String, msg: String) {
        println("D/$tag: $msg")
    }
    actual fun w(tag: String, msg: String) {
        println("W/$tag: $msg")
    }
    actual fun e(tag: String, msg: String, t: Throwable?) {
        System.err.println("E/$tag: $msg")
        t?.printStackTrace(System.err)
    }
}
