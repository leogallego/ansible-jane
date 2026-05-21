package com.example.aapremote

import com.example.aapremote.model.AapInstance
import com.example.aapremote.model.Job
import com.example.aapremote.model.JobStatus
import com.example.aapremote.model.JobSummaryFields
import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.JobTemplateSummaryFields
import com.example.aapremote.model.JobTemplateRef
import com.example.aapremote.model.Label
import com.example.aapremote.model.LabelSummary
import com.example.aapremote.model.WorkflowJob
import com.example.aapremote.model.WorkflowJobSummaryFields
import com.example.aapremote.model.WorkflowJobTemplate
import com.example.aapremote.model.WorkflowJobTemplateRef
import com.example.aapremote.model.WorkflowNode
import com.example.aapremote.model.WorkflowNodeJob
import com.example.aapremote.model.WorkflowNodeSummaryFields
import com.example.aapremote.model.EdaRuleAudit
import com.example.aapremote.model.Host
import com.example.aapremote.model.Inventory
import com.example.aapremote.model.Schedule
import com.example.aapremote.model.ScheduleSummaryFields
import com.example.aapremote.model.UnifiedJobTemplateRef
import com.example.aapremote.model.WorkflowTemplateSummaryFields

val testInstance = AapInstance(
    id = "test-instance-1",
    baseUrl = "https://aap.example.com",
    token = "test-token-123"
)

val testInstance2 = AapInstance(
    id = "test-instance-2",
    baseUrl = "https://aap2.example.com",
    token = "test-token-456"
)

val testLabel1 = Label(id = 1, name = "production")
val testLabel2 = Label(id = 2, name = "staging")

fun testJobTemplate(
    id: Int = 1,
    name: String = "Deploy App",
    description: String = "Deploy the application",
    askVariablesOnLaunch: Boolean = false,
    labels: List<Label> = emptyList()
) = JobTemplate(
    id = id,
    name = name,
    description = description,
    askVariablesOnLaunch = askVariablesOnLaunch,
    summaryFields = JobTemplateSummaryFields(
        labels = LabelSummary(count = labels.size, results = labels)
    )
)

fun testJob(
    id: Int = 1,
    name: String = "Deploy App",
    status: JobStatus = JobStatus.RUNNING,
    failed: Boolean = false,
    started: String? = "2024-01-15T10:00:00Z",
    finished: String? = null,
    elapsed: Double? = null,
    templateId: Int = 1,
    templateName: String = "Deploy App"
) = Job(
    id = id,
    name = name,
    status = status,
    failed = failed,
    started = started,
    finished = finished,
    elapsed = elapsed,
    summaryFields = JobSummaryFields(
        jobTemplate = JobTemplateRef(id = templateId, name = templateName)
    )
)

fun testWorkflowJobTemplate(
    id: Int = 1,
    name: String = "Full Deploy Pipeline",
    description: String = "Complete deployment workflow",
    askVariablesOnLaunch: Boolean = false,
    labels: List<Label> = emptyList()
) = WorkflowJobTemplate(
    id = id,
    name = name,
    description = description,
    askVariablesOnLaunch = askVariablesOnLaunch,
    summaryFields = WorkflowTemplateSummaryFields(
        labels = LabelSummary(count = labels.size, results = labels)
    )
)

fun testWorkflowJob(
    id: Int = 1,
    name: String = "Full Deploy Pipeline",
    status: JobStatus = JobStatus.RUNNING,
    failed: Boolean = false,
    started: String? = "2024-01-15T10:00:00Z",
    finished: String? = null,
    elapsed: Double? = null,
    templateId: Int = 1,
    templateName: String = "Full Deploy Pipeline"
) = WorkflowJob(
    id = id,
    name = name,
    status = status,
    failed = failed,
    started = started,
    finished = finished,
    elapsed = elapsed,
    summaryFields = WorkflowJobSummaryFields(
        workflowJobTemplate = WorkflowJobTemplateRef(id = templateId, name = templateName)
    )
)

fun testWorkflowNode(
    id: Int = 1,
    jobId: Int = 10,
    jobName: String = "Step 1",
    jobStatus: JobStatus = JobStatus.SUCCESSFUL,
    doNotRun: Boolean = false
) = WorkflowNode(
    id = id,
    summaryFields = WorkflowNodeSummaryFields(
        job = WorkflowNodeJob(id = jobId, name = jobName, status = jobStatus)
    ),
    doNotRun = doNotRun
)

object TestData {
    val testInstance = com.example.aapremote.testInstance
    val testInstance2 = com.example.aapremote.testInstance2

    fun createInventory(id: Int, name: String = "Inventory $id") = Inventory(
        id = id,
        name = name,
        description = "Test inventory $id",
        totalHosts = id * 10,
        totalGroups = id * 2
    )

    fun createHost(id: Int, name: String = "host-$id.example.com") = Host(
        id = id,
        name = name,
        description = "Test host $id",
        enabled = true,
        inventory = 1
    )

    fun createSchedule(
        id: Int,
        name: String = "Schedule $id",
        enabled: Boolean = true
    ) = Schedule(
        id = id,
        name = name,
        enabled = enabled,
        rrule = "DTSTART:20240101T000000Z RRULE:FREQ=DAILY",
        summaryFields = ScheduleSummaryFields(
            unifiedJobTemplate = UnifiedJobTemplateRef(
                id = id * 10,
                name = "Template for $name"
            )
        )
    )

    fun createEdaRuleAudit(id: Int, name: String = "Rule Audit $id") = EdaRuleAudit(
        id = id,
        name = name,
        status = "successful",
        firedAt = "2024-01-01T00:00:00Z",
        ruleName = "rule-$id",
        ruleSetName = "ruleset-$id",
        activationName = "activation-$id"
    )

    val sampleInventories = (1..3).map { createInventory(it) }
    val sampleHosts = (1..3).map { createHost(it) }
    val sampleSchedules = (1..3).map { createSchedule(it) }
    val sampleEdaRuleAudits = (1..3).map { createEdaRuleAudit(it) }
}
