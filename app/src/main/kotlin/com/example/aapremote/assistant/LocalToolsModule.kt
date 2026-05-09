package com.example.aapremote.assistant

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.local.GetHostFactsLocalTool
import com.example.aapremote.assistant.tools.local.GetJobLocalTool
import com.example.aapremote.assistant.tools.local.GetJobStdoutLocalTool
import com.example.aapremote.assistant.tools.local.GetWorkflowJobLocalTool
import com.example.aapremote.assistant.tools.local.LaunchJobLocalTool
import com.example.aapremote.assistant.tools.local.LaunchWorkflowLocalTool
import com.example.aapremote.assistant.tools.local.ListEdaAuditRulesLocalTool
import com.example.aapremote.assistant.tools.local.ListHostsLocalTool
import com.example.aapremote.assistant.tools.local.ListInventoriesLocalTool
import com.example.aapremote.assistant.tools.local.ListJobTemplatesLocalTool
import com.example.aapremote.assistant.tools.local.ListJobsLocalTool
import com.example.aapremote.assistant.tools.local.ListSchedulesLocalTool
import com.example.aapremote.assistant.tools.local.ListWorkflowTemplatesLocalTool
import com.example.aapremote.assistant.tools.local.ToggleScheduleLocalTool
import org.koin.dsl.bind
import org.koin.dsl.module

val localToolsModule = module {
    single { ListJobTemplatesLocalTool(get()) } bind LocalTool::class
    single { LaunchJobLocalTool(get()) } bind LocalTool::class
    single { GetJobLocalTool(get()) } bind LocalTool::class
    single { GetJobStdoutLocalTool(get()) } bind LocalTool::class
    single { ListJobsLocalTool(get()) } bind LocalTool::class
    single { ListWorkflowTemplatesLocalTool(get()) } bind LocalTool::class
    single { LaunchWorkflowLocalTool(get()) } bind LocalTool::class
    single { GetWorkflowJobLocalTool(get()) } bind LocalTool::class
    single { ListInventoriesLocalTool(get()) } bind LocalTool::class
    single { ListHostsLocalTool(get()) } bind LocalTool::class
    single { GetHostFactsLocalTool(get()) } bind LocalTool::class
    single { ListSchedulesLocalTool(get()) } bind LocalTool::class
    single { ToggleScheduleLocalTool(get()) } bind LocalTool::class
    single { ListEdaAuditRulesLocalTool(get()) } bind LocalTool::class
}
