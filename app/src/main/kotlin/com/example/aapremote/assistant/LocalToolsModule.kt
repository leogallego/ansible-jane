package com.example.aapremote.assistant

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.local.GetHostFactsLocalTool
import com.example.aapremote.assistant.tools.local.GetJobLocalTool
import com.example.aapremote.assistant.tools.local.GetJobStdoutLocalTool
import com.example.aapremote.assistant.tools.local.GetWorkflowJobLocalTool
import com.example.aapremote.assistant.tools.local.LaunchJobLocalTool
import com.example.aapremote.assistant.tools.local.LaunchWorkflowLocalTool
import com.example.aapremote.assistant.tools.local.GetCredentialLocalTool
import com.example.aapremote.assistant.tools.local.GetEdaActivationLocalTool
import com.example.aapremote.assistant.tools.local.GetInstanceLocalTool
import com.example.aapremote.assistant.tools.local.GetMeshTopologyLocalTool
import com.example.aapremote.assistant.tools.local.GetProjectLocalTool
import com.example.aapremote.assistant.tools.local.ListCredentialsLocalTool
import com.example.aapremote.assistant.tools.local.ListEdaActivationsLocalTool
import com.example.aapremote.assistant.tools.local.ListEdaAuditRulesLocalTool
import com.example.aapremote.assistant.tools.local.ListExecutionEnvironmentsLocalTool
import com.example.aapremote.assistant.tools.local.ListHostsLocalTool
import com.example.aapremote.assistant.tools.local.ListInstanceGroupsLocalTool
import com.example.aapremote.assistant.tools.local.ListInstancesLocalTool
import com.example.aapremote.assistant.tools.local.ListInventoriesLocalTool
import com.example.aapremote.assistant.tools.local.ListJobTemplatesLocalTool
import com.example.aapremote.assistant.tools.local.ListJobsLocalTool
import com.example.aapremote.assistant.tools.local.ListProjectsLocalTool
import com.example.aapremote.assistant.tools.local.ListSchedulesLocalTool
import com.example.aapremote.assistant.tools.local.ListWorkflowTemplatesLocalTool
import com.example.aapremote.assistant.tools.local.PingLocalTool
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
    single { ListInstancesLocalTool(get()) } bind LocalTool::class
    single { GetInstanceLocalTool(get()) } bind LocalTool::class
    single { ListInstanceGroupsLocalTool(get()) } bind LocalTool::class
    single { PingLocalTool(get()) } bind LocalTool::class
    single { GetMeshTopologyLocalTool(get()) } bind LocalTool::class
    single { ListCredentialsLocalTool(get()) } bind LocalTool::class
    single { GetCredentialLocalTool(get()) } bind LocalTool::class
    single { ListProjectsLocalTool(get()) } bind LocalTool::class
    single { GetProjectLocalTool(get()) } bind LocalTool::class
    single { ListExecutionEnvironmentsLocalTool(get()) } bind LocalTool::class
    single { ListEdaActivationsLocalTool(get()) } bind LocalTool::class
    single { GetEdaActivationLocalTool(get()) } bind LocalTool::class
}
