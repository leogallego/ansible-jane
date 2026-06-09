package io.github.leogallego.ansiblejane.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.leogallego.ansiblejane.model.WorkflowApproval

actual class NotificationManager(private val context: Context) {

    actual fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for pending workflow approvals"
        }
        val notificationManager =
            context.getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    actual fun showApprovalNotification(approval: WorkflowApproval): Boolean {
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
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            setPackage(context.packageName)
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(approval.name)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(approval.id, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "workflow_approvals"
        const val CHANNEL_NAME = "Workflow Approvals"
    }
}
