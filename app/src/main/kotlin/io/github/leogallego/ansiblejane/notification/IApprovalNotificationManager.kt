package io.github.leogallego.ansiblejane.notification

import android.content.Context
import io.github.leogallego.ansiblejane.model.WorkflowApproval

interface IApprovalNotificationManager {
    fun showNotification(context: Context, approval: WorkflowApproval): Boolean
}
