package com.example.aapremote.presentation

import com.example.aapremote.presentation.auth.AuthViewModel
import com.example.aapremote.presentation.settings.BackupViewModel
import com.example.aapremote.presentation.jobs.JobStatusViewModel
import com.example.aapremote.presentation.jobs.RecentJobsViewModel
import com.example.aapremote.presentation.eda.EdaAuditViewModel
import com.example.aapremote.presentation.hosts.HostsViewModel
import com.example.aapremote.presentation.hosts.InventoryHostsViewModel
import com.example.aapremote.presentation.inventory.InventoriesViewModel
import com.example.aapremote.presentation.schedules.SchedulesViewModel
import com.example.aapremote.presentation.settings.SettingsViewModel
import com.example.aapremote.presentation.templates.TemplatesViewModel
import com.example.aapremote.presentation.workflows.WorkflowJobStatusViewModel
import com.example.aapremote.presentation.workflows.WorkflowTemplatesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::TemplatesViewModel)
    viewModelOf(::JobStatusViewModel)
    viewModelOf(::RecentJobsViewModel)
    viewModelOf(::WorkflowTemplatesViewModel)
    viewModelOf(::WorkflowJobStatusViewModel)
    viewModelOf(::SchedulesViewModel)
    viewModelOf(::EdaAuditViewModel)
    viewModelOf(::InventoriesViewModel)
    viewModelOf(::InventoryHostsViewModel)
    viewModelOf(::HostsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::BackupViewModel)
}
