package io.github.leogallego.ansiblejane.assistant

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetHostFactsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetJobLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetJobStdoutLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetWorkflowJobLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.LaunchJobLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.LaunchWorkflowLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetCredentialLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetEdaActivationLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetInstanceLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetMeshTopologyLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.GetProjectLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListCredentialsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListEdaActivationsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListEdaAuditRulesLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListExecutionEnvironmentsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListHostsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListInstanceGroupsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListInstancesLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListInventoriesLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListJobTemplatesLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListJobsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListProjectsLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListSchedulesLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListWorkflowTemplatesLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.PingLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ToggleScheduleLocalTool
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
