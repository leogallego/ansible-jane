package io.github.leogallego.ansiblejane.platform

actual class BackgroundWorker {
    actual fun schedulePolling(intervalMinutes: Long) {
        // Desktop polling: no-op stub. Phase 7 adds ScheduledExecutorService.
    }

    actual fun cancelPolling() {
        // No-op stub
    }
}
