package io.github.leogallego.ansiblejane.platform

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

actual class BackgroundWorker {
    private val lock = ReentrantLock()
    private var executor: ScheduledExecutorService? = null
    private var scheduledTask: ScheduledFuture<*>? = null

    actual fun schedulePolling(intervalMinutes: Long) {
        lock.withLock {
            cancelPollingInternal()

            val newExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "approval-polling").apply {
                    isDaemon = true
                }
            }

            val task = newExecutor.scheduleAtFixedRate(
                { println("Desktop approval polling tick") },
                intervalMinutes,
                intervalMinutes,
                TimeUnit.MINUTES
            )

            executor = newExecutor
            scheduledTask = task
        }
    }

    actual fun cancelPolling() {
        lock.withLock {
            cancelPollingInternal()
        }
    }

    private fun cancelPollingInternal() {
        scheduledTask?.cancel(false)
        scheduledTask = null

        executor?.let {
            it.shutdown()
            try {
                if (!it.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    it.shutdownNow()
                }
            } catch (e: InterruptedException) {
                it.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        executor = null
    }
}
