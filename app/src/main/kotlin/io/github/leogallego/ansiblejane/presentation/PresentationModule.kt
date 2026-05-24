package io.github.leogallego.ansiblejane.presentation

import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import io.github.leogallego.ansiblejane.presentation.jobs.JobStatusViewModel
import io.github.leogallego.ansiblejane.presentation.jobs.RecentJobsViewModel
import io.github.leogallego.ansiblejane.presentation.eda.EdaAuditViewModel
import io.github.leogallego.ansiblejane.presentation.hosts.HostsViewModel
import io.github.leogallego.ansiblejane.presentation.hosts.InventoryHostsViewModel
import io.github.leogallego.ansiblejane.presentation.inventory.InventoriesViewModel
import io.github.leogallego.ansiblejane.presentation.schedules.SchedulesViewModel
import io.github.leogallego.ansiblejane.presentation.templates.TemplatesViewModel
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowJobStatusViewModel
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplateDetailViewModel
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplatesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::TemplatesViewModel)
    viewModelOf(::JobStatusViewModel)
    viewModelOf(::RecentJobsViewModel)
    viewModelOf(::WorkflowTemplatesViewModel)
    viewModelOf(::WorkflowJobStatusViewModel)
    viewModelOf(::WorkflowTemplateDetailViewModel)
    viewModelOf(::SchedulesViewModel)
    viewModelOf(::EdaAuditViewModel)
    viewModelOf(::InventoriesViewModel)
    viewModelOf(::InventoryHostsViewModel)
    viewModelOf(::HostsViewModel)
    viewModelOf(::BackupViewModel)
}
