package io.github.leogallego.ansiblejane.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ApprovalActionReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_APPROVE = "io.github.leogallego.ansiblejane.ACTION_APPROVE"
        const val ACTION_DENY = "io.github.leogallego.ansiblejane.ACTION_DENY"
        const val EXTRA_APPROVAL_ID = "approval_id"
        private const val TAG = "ApprovalActionReceiver"
    }

    private fun safeNotificationId(approvalId: Int): Int =
        if (approvalId == ApprovalNotificationManager.SUMMARY_ID) approvalId + 1 else approvalId

    private fun showErrorNotification(
        context: Context,
        approvalId: Int,
        wasApprove: Boolean,
        errorMessage: String?
    ) {
        val actionLabel = if (wasApprove) "approve" else "deny"
        val retryIntent = Intent(context, ApprovalActionReceiver::class.java).apply {
            action = if (wasApprove) ACTION_APPROVE else ACTION_DENY
            putExtra(EXTRA_APPROVAL_ID, approvalId)
        }
        val retryRequestCode = (approvalId.hashCode() and 0x1FFFFFFF) or 0x40000000
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            retryRequestCode,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ApprovalNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_approval)
            .setContentTitle(context.getString(R.string.notification_approval_failed_title, actionLabel))
            .setContentText(errorMessage ?: context.getString(R.string.notification_approval_failed_message))
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification_approval, context.getString(R.string.action_retry), retryPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(safeNotificationId(approvalId), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot post error notification — channel may have been deleted", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_APPROVE && intent.action != ACTION_DENY) return

        val approvalId = intent.getIntExtra(EXTRA_APPROVAL_ID, -1)
        if (approvalId <= 0) return

        val isApprove = intent.action == ACTION_APPROVE
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            val actionLabel = if (isApprove) "approve" else "deny"
            try {
                val tokenManager: ITokenManager = get()
                val instance = withTimeoutOrNull(5_000L) {
                    tokenManager.activeInstance.firstOrNull { it != null }
                }
                if (instance == null) {
                    Log.w(TAG, "No active instance, cannot $actionLabel $approvalId")
                    showErrorNotification(
                        context, approvalId, isApprove,
                        context.getString(R.string.notification_no_active_instance)
                    )
                    return@launch
                }

                val apiProvider: IAapApiProvider = get()
                if (isApprove) {
                    apiProvider.getApiService().approveWorkflow(approvalId)
                } else {
                    apiProvider.getApiService().denyWorkflow(approvalId)
                }

                NotificationManagerCompat.from(context).cancel(safeNotificationId(approvalId))

                Log.i(TAG, "Approval $approvalId ${actionLabel}d")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to $actionLabel $approvalId", e)
                showErrorNotification(context, approvalId, isApprove, e.message)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
