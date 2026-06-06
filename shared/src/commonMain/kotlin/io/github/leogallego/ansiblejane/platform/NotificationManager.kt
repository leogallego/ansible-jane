package io.github.leogallego.ansiblejane.platform

import io.github.leogallego.ansiblejane.model.WorkflowApproval

expect class NotificationManager {
    fun showApprovalNotification(approval: WorkflowApproval): Boolean
    fun createChannel()
}
