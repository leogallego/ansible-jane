package com.example.aapremote.presentation

import com.example.aapremote.presentation.auth.AuthViewModel
import com.example.aapremote.presentation.jobs.JobStatusViewModel
import com.example.aapremote.presentation.jobs.RecentJobsViewModel
import com.example.aapremote.presentation.templates.TemplatesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::TemplatesViewModel)
    viewModelOf(::JobStatusViewModel)
    viewModelOf(::RecentJobsViewModel)
}
