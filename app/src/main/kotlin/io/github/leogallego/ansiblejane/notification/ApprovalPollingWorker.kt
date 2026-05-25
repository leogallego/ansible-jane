package io.github.leogallego.ansiblejane.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.network.AapApiProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ApprovalPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "ApprovalPollingWorker"
        const val WORK_NAME = "approval_polling"
    }

    override suspend fun doWork(): Result {
        return try {
            val tokenManager: ITokenManager = get()
            if (tokenManager.activeInstance.value == null) {
                return Result.success()
            }
            val apiProvider: AapApiProvider = get()
            val approvalTracker: ApprovalTracker = get()
            val notificationManager: ApprovalNotificationManager = get()

            val response = apiProvider.getApiService().getWorkflowApprovals(status = "pending")
            val pendingApprovals = response.results
            val pendingIds = pendingApprovals.map { it.id }.toSet()

            val seenIds = approvalTracker.getSeenIds()
            val newIds = pendingIds - seenIds

            for (approval in pendingApprovals) {
                if (approval.id in newIds) {
                    notificationManager.showNotification(applicationContext, approval)
                }
            }

            if (newIds.isNotEmpty()) {
                approvalTracker.markSeen(newIds)
            }

            // Prune IDs that are no longer pending
            approvalTracker.pruneIds(pendingIds)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll for workflow approvals", e)
            Result.retry()
        }
    }
}
