package io.github.leogallego.ansiblejane.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.leogallego.ansiblejane.MainActivity
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.model.WorkflowApproval

class ApprovalNotificationManager {

    companion object {
        const val CHANNEL_ID = "workflow_approvals"
        const val CHANNEL_NAME = "Workflow Approvals"
        private const val GROUP_KEY = "io.github.leogallego.ansiblejane.APPROVAL_GROUP"
        private const val SUMMARY_ID = 0

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for pending workflow approvals"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, approval: WorkflowApproval): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        val deepLinkUri = Uri.parse("ansiblejane://approval/${approval.id}")
        val contentIntent = Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            approval.id,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val approveIntent = Intent(context, ApprovalActionReceiver::class.java).apply {
            action = ApprovalActionReceiver.ACTION_APPROVE
            putExtra(ApprovalActionReceiver.EXTRA_APPROVAL_ID, approval.id)
        }
        val approvePendingIntent = PendingIntent.getBroadcast(
            context,
            approval.id * 2,
            approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val denyIntent = Intent(context, ApprovalActionReceiver::class.java).apply {
            action = ApprovalActionReceiver.ACTION_DENY
            putExtra(ApprovalActionReceiver.EXTRA_APPROVAL_ID, approval.id)
        }
        val denyPendingIntent = PendingIntent.getBroadcast(
            context,
            approval.id * 2 + 1,
            denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val templateName = approval.summaryFields.workflowJobTemplate?.name
        val bodyText = if (templateName != null) {
            "Pending approval for $templateName"
        } else {
            "Pending workflow approval"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_approval)
            .setContentTitle(approval.name)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .addAction(R.drawable.ic_notification_approval, "Approve", approvePendingIntent)
            .addAction(0, "Deny", denyPendingIntent)
            .build()

        val manager = NotificationManagerCompat.from(context)
        manager.notify(approval.id, notification)

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_approval)
            .setContentTitle("Workflow Approvals")
            .setContentText("Pending workflow approvals")
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        manager.notify(SUMMARY_ID, summaryNotification)

        return true
    }
}
