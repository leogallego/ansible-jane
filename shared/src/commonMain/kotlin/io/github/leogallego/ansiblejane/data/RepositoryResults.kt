package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Credential
import io.github.leogallego.ansiblejane.model.EdaActivation
import io.github.leogallego.ansiblejane.model.EdaRuleAudit
import io.github.leogallego.ansiblejane.model.ExecutionEnvironment
import io.github.leogallego.ansiblejane.model.Host
import io.github.leogallego.ansiblejane.model.Instance
import io.github.leogallego.ansiblejane.model.InstanceGroup
import io.github.leogallego.ansiblejane.model.Inventory
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.JobHostSummary
import io.github.leogallego.ansiblejane.model.JobTemplate
import io.github.leogallego.ansiblejane.model.Project
import io.github.leogallego.ansiblejane.model.Schedule
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplate

data class CredentialListResult(
    val credentials: List<Credential>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class EdaActivationListResult(
    val activations: List<EdaActivation>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class EdaAuditResult(
    val auditRules: List<EdaRuleAudit>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class ExecutionEnvironmentListResult(
    val executionEnvironments: List<ExecutionEnvironment>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class HostListResult(
    val hosts: List<Host>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class InstanceGroupListResult(
    val instanceGroups: List<InstanceGroup>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class InstanceListResult(
    val instances: List<Instance>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class InventoryListResult(
    val inventories: List<Inventory>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class JobHostSummaryResult(
    val summaries: List<JobHostSummary>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class PendingApprovalResult(
    val approvals: List<WorkflowApproval>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class ProjectListResult(
    val projects: List<Project>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class RecentJobsResult(
    val jobs: List<Job>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class SchedulesResult(
    val schedules: List<Schedule>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class TemplateListResult(
    val templates: List<JobTemplate>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class WorkflowTemplateListResult(
    val templates: List<WorkflowJobTemplate>,
    val hasMore: Boolean,
    val totalCount: Int
)
