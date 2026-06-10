package io.github.leogallego.ansiblejane.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
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
            val instance = withTimeoutOrNull(5_000L) {
                tokenManager.activeInstance.firstOrNull { it != null }
            }
            if (instance == null) {
                return Result.success()
            }
            val userPreferences: IUserPreferencesRepository = get()
            val pollingEnabled = withTimeoutOrNull(2_000L) {
                userPreferences.approvalPollingEnabled(instance.id).firstOrNull()
            } ?: true
            if (!pollingEnabled) {
                return Result.success()
            }
            val apiProvider: IAapApiProvider = get()
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

            // Mark all new IDs as seen regardless of notification delivery outcome.
            // Tracking only delivered IDs causes infinite retry when permission is denied.
            if (newIds.isNotEmpty()) {
                approvalTracker.markSeen(newIds)
            }

            // Prune IDs that are no longer pending
            approvalTracker.pruneIds(pendingIds)

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll for workflow approvals", e)
            Result.retry()
        }
    }
}
