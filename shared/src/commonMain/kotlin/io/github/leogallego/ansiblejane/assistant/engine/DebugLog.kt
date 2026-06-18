package io.github.leogallego.ansiblejane.assistant.engine

expect object DebugLog {
    var enabled: Boolean
    fun d(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String, t: Throwable? = null)
}
