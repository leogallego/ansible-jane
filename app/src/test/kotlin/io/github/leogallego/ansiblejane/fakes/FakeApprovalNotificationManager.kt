package io.github.leogallego.ansiblejane.fakes

import android.content.Context
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.notification.IApprovalNotificationManager

class FakeApprovalNotificationManager : IApprovalNotificationManager {
    val shownApprovals = mutableListOf<WorkflowApproval>()
    var returnValue = true

    override fun showNotification(context: Context, approval: WorkflowApproval): Boolean {
        shownApprovals.add(approval)
        return returnValue
    }
}
