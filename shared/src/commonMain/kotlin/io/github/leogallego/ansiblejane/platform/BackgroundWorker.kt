package io.github.leogallego.ansiblejane.platform

expect class BackgroundWorker {
    fun schedulePolling(intervalMinutes: Long)
    fun cancelPolling()
}
