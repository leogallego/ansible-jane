package io.github.leogallego.ansiblejane.platform

import io.github.leogallego.ansiblejane.model.WorkflowApproval

actual class NotificationManager {
    actual fun showApprovalNotification(approval: WorkflowApproval): Boolean {
        // Desktop notifications: no-op stub
        return false
    }

    actual fun createChannel() {
        // No-op on desktop
    }
}
