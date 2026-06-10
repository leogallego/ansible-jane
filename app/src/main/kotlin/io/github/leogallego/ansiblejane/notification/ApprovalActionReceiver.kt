package io.github.leogallego.ansiblejane.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    override fun onReceive(context: Context, intent: Intent) {
        val approvalId = intent.getIntExtra(EXTRA_APPROVAL_ID, -1)
        if (approvalId <= 0) return

        val isApprove = intent.action == ACTION_APPROVE
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val tokenManager: ITokenManager = get()
                val instance = withTimeoutOrNull(5_000L) {
                    tokenManager.activeInstance.firstOrNull { it != null }
                }
                if (instance == null) {
                    Log.w(TAG, "No active instance, cannot ${if (isApprove) "approve" else "deny"} $approvalId")
                    return@launch
                }

                val apiProvider: IAapApiProvider = get()
                if (isApprove) {
                    apiProvider.getApiService().approveWorkflow(approvalId)
                } else {
                    apiProvider.getApiService().denyWorkflow(approvalId)
                }

                NotificationManagerCompat.from(context).cancel(approvalId)

                Log.i(TAG, "Approval $approvalId ${if (isApprove) "approved" else "denied"}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ${if (isApprove) "approve" else "deny"} $approvalId", e)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
