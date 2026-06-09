package io.github.leogallego.ansiblejane.assistant.engine

object DebugLog {
    fun d(tag: String, msg: String) {
        println("D/$tag: $msg")
    }
    fun w(tag: String, msg: String) {
        println("W/$tag: $msg")
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        println("E/$tag: $msg")
        t?.printStackTrace()
    }
}
