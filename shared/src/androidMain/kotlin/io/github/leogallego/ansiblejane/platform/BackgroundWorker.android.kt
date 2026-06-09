package io.github.leogallego.ansiblejane.platform

import android.content.Context
import androidx.work.WorkManager

actual class BackgroundWorker(private val context: Context) {

    actual fun schedulePolling(intervalMinutes: Long) {
        // Worker registration handled by AnsibleJaneApp.onCreate() which directly
        // enqueues ApprovalPollingWorker. This abstraction becomes the primary
        // scheduling entry point in Phase 4 when polling logic moves to shared.
    }

    actual fun cancelPolling() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "approval_polling"
    }
}
