package io.github.leogallego.ansiblejane.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        const val EXTRA_APPROVAL_ID = "approval_id"

        /**
         * Creates the notification channel. Safe to call multiple times.
         * Should be called from Application.onCreate().
         */
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

    /**
     * Shows a notification for a pending workflow approval.
     * Each approval gets a unique notification ID based on its API id.
     */
    fun showNotification(context: Context, approval: WorkflowApproval) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_APPROVAL_ID, approval.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            approval.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val templateName = approval.summaryFields.workflowJobTemplate?.name
        val bodyText = if (templateName != null) {
            "Pending workflow approval for $templateName"
        } else {
            "Pending workflow approval"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(approval.name)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(approval.id, notification)
    }
}
