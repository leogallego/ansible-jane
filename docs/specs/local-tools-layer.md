# Local Tools Layer — AI Assistant Direct API Access

**Issue:** #54
**Related:** #49 (result summarization), #52 (MCP toolsets), #34 (Phase B confirmation), #48 (MCP SDK migration)
**Date:** 2026-05-09

## 1. Problem Statement

The AI assistant currently routes all tool calls through external MCP servers, even for operations the app already implements via Retrofit (12+ endpoints across `AapApiService` and `EdaApiService`). This creates unnecessary latency, external dependency, and token waste.

### Current flow (all operations)

```
User query → ChatEngine → LLM → tool_call
  → McpTool → HTTP to MCP server → MCP server calls AAP API → response
  → back to LLM → answer
```

### Proposed flow (overlapping operations)

```
User query → ChatEngine → LLM → tool_call
  → LocalTool → Repository → Retrofit → AAP API → response
  → back to LLM → answer
```

For operations only MCP provides, the existing MCP flow remains unchanged.

## 2. Architecture

### 2.1 Tool interface (existing)

```kotlin
// assistant/tools/ToolSpec.kt — no changes needed
interface Tool {
    val spec: ToolSpec
    suspend fun execute(args: Map<String, Any>): ToolResult
}
```

### 2.2 New: LocalTool base class

```kotlin
// assistant/tools/LocalTool.kt
abstract class LocalTool(
    override val spec: ToolSpec,
    val destructive: Boolean = false
) : Tool {
    val source: ToolSource = ToolSource.LOCAL
}

enum class ToolSource { LOCAL, MCP }
```

Each local tool is a thin wrapper around an existing repository. Example:

```kotlin
// assistant/tools/local/ListJobTemplatesLocalTool.kt
class ListJobTemplatesLocalTool(
    private val templateRepository: TemplateRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_job_templates",
        description = "List job templates with optional search and label filter",
        parametersSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {
                putJsonObject("search") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Search term to filter templates by name"))
                }
                putJsonObject("labels") {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by label name (case-insensitive contains)"))
                }
                putJsonObject("page_size") {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Number of results per page (default 10, max 25)"))
                }
            }
        }
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val search = args["search"] as? String
            val labels = args["labels"] as? String
            val pageSize = (args["page_size"] as? Number)?.toInt()?.coerceIn(1, 25) ?: 10
            val response = templateRepository.getJobTemplates(
                search = search,
                labelsFilter = labels,
                pageSize = pageSize
            )
            ToolResult(success = true, data = response.toJson())
        } catch (e: Exception) {
            ToolResult(success = false, data = e.message, errorType = ErrorType.SERVER_ERROR)
        }
    }
}
```

### 2.3 ToolRouter evolution

Current `ToolRouter` is a static object that only filters MCP tools by keyword. It evolves into a **tool registry** that:

1. Registers tools from all sources (local + MCP)
2. Resolves conflicts (local wins by default for overlapping capabilities)
3. Supports per-tool enable/disable
4. Maintains read-only access control

```kotlin
// assistant/engine/ToolRouter.kt — evolved
class ToolRouter {

    data class RegisteredTool(
        val tool: Tool,
        val source: ToolSource,
        val enabled: Boolean = true,
        val overlapGroup: String? = null  // e.g., "job_templates_list"
    )

    private val registry = mutableListOf<RegisteredTool>()

    fun registerLocalTools(tools: List<LocalTool>) { ... }
    fun registerMcpTools(tools: List<McpTool>, serverConfig: McpServerConfig) { ... }

    fun getToolsForQuery(
        query: String,
        serverConfigs: List<McpServerConfig>
    ): List<Tool> {
        // 1. Filter by keyword/category (existing logic)
        // 2. For each overlap group, prefer LOCAL if enabled
        // 3. Apply read-only filtering for write tools
        // 4. Return deduplicated tool list
    }

    fun setToolEnabled(toolName: String, source: ToolSource, enabled: Boolean) { ... }
}
```

### 2.4 Overlap resolution

When both a local tool and an MCP tool provide the same capability:

| Scenario | Behavior |
|---|---|
| Both exist, default config | Local tool active, MCP tool disabled |
| User enables MCP override | MCP tool active, local tool disabled (per overlap group, only one active) |
| MCP server unavailable | Local tool always available as fallback |
| MCP tool has no local equivalent | MCP tool active (no conflict) |

Overlap groups map local tool names to MCP tool name patterns:

```kotlin
val OVERLAP_MAPPING = mapOf(
    "list_job_templates" to "controller.job_templates_list",
    "launch_job" to "controller.job_templates_launch_create",
    "get_job" to "controller.jobs_read",
    "list_jobs" to "controller.jobs_list",
    "list_workflow_templates" to "controller.workflow_job_templates_list",
    "launch_workflow" to "controller.workflow_job_templates_launch_create",
    "get_workflow_job" to "controller.workflow_jobs_read",
    "list_inventories" to "controller.inventories_list",
    "list_hosts" to "controller.hosts_list",
    "get_host_facts" to "controller.hosts_ansible_facts_read",
    "list_schedules" to "controller.schedules_list",
    "toggle_schedule" to "controller.schedules_update",
    "list_eda_audit_rules" to "eda.audit_rules_list",
    "get_eda_audit_rule" to "eda.audit_rules_read",
)
```

### 2.5 Write operation confirmation

All tools with `destructive = true` trigger the confirmation flow defined in #34:

```kotlin
// In ChatEngine, before executing any tool:
if (tool is LocalTool && tool.destructive) {
    emit(ChatEvent.ConfirmationRequired(tool.spec.name, args))
    // Wait for user approval/denial
}
// Same logic for McpTool with destructive annotations
```

Future enhancement: per-tool "don't ask again" toggle stored in DataStore preferences.

## 3. Tool Inventory

### 3.1 Tier 1 — Wrap existing Retrofit repositories (no new API code)

These use existing `AapApiService` and `EdaApiService` endpoints.

| # | Local tool name | Repository | Method(s) | Type | MCP overlap |
|---|---|---|---|---|---|
| 1 | `list_job_templates` | TemplateRepository | `getJobTemplates()` | Read | `controller.job_templates_list` |
| 2 | `launch_job` | TemplateRepository | `launchJob()` | **Write** | `controller.job_templates_launch_create` |
| 3 | `get_job` | JobRepository | `getJob()` | Read | `controller.jobs_read` |
| 4 | `get_job_stdout` | JobRepository | `getJobStdout()` | Read | `controller.jobs_stdout_read` |
| 5 | `list_jobs` | JobRepository | `getJobs()` | Read | `controller.jobs_list` |
| 6 | `list_workflow_templates` | WorkflowRepository | `getWorkflowJobTemplates()` | Read | `controller.workflow_job_templates_list` |
| 7 | `launch_workflow` | WorkflowRepository | `launchWorkflowJob()` | **Write** | `controller.workflow_job_templates_launch_create` |
| 8 | `get_workflow_job` | WorkflowRepository | `getWorkflowJob()` + `getWorkflowNodes()` | Read | `controller.workflow_jobs_read` |
| 9 | `list_inventories` | InventoryRepository | `getInventories()` | Read | `controller.inventories_list` |
| 10 | `list_hosts` | HostRepository | `getHosts()` + `getInventoryHosts()` | Read | `controller.hosts_list` |
| 11 | `get_host_facts` | HostRepository | `getHostFacts()` | Read | `controller.hosts_ansible_facts_read` |
| 12 | `list_schedules` | ScheduleRepository | `getSchedules()` | Read | `controller.schedules_list` |
| 13 | `toggle_schedule` | ScheduleRepository | `toggleSchedule()` | **Write** | `controller.schedules_update` |
| 14 | `list_eda_audit_rules` | EdaAuditRepository | `getAuditRules()` + `getAuditRule()` | Read | `eda.audit_rules_list` |

### 3.2 Tier 2 — New Retrofit endpoints + local tools

New endpoints added to `AapApiService` or `EdaApiService`, then wrapped as local tools.

**Priority order (read-only first):**

| # | Local tool name | API endpoint | Type | Use case | Priority |
|---|---|---|---|---|---|
| 15 | `list_instances` | `GET /api/v2/instances/` | Read | Cluster health overview | P1 |
| 16 | `get_instance` | `GET /api/v2/instances/{id}/` | Read | Instance capacity, node type, health | P1 |
| 17 | `list_instance_groups` | `GET /api/v2/instance_groups/` | Read | Which instance group runs prod jobs | P1 |
| 18 | `ping` | `GET /api/v2/ping/` | Read | Quick cluster health check | P1 |
| 19 | `get_topology` | `GET /api/v2/mesh_visualizer/` | Read | Automation mesh node/edge map | P1 |
| 20 | `list_credentials` | `GET /api/v2/credentials/` | Read | Context: what credential does template X use | P2 |
| 21 | `get_credential` | `GET /api/v2/credentials/{id}/` | Read | Credential type, org (no secrets exposed) | P2 |
| 22 | `list_projects` | `GET /api/v2/projects/` | Read | Project list with sync status | P2 |
| 23 | `get_project` | `GET /api/v2/projects/{id}/` | Read | Last sync time, SCM URL, branch, status | P2 |
| 24 | `list_execution_environments` | `GET /api/v2/execution_environments/` | Read | Which EE does template X use | P2 |
| 25 | `list_organizations` | `GET /api/v2/organizations/` | Read | Multi-org context | P3 |
| 26 | `list_labels` | `GET /api/v2/labels/` | Read | Standalone label listing for filtering | P3 |
| 27 | `list_eda_rulebook_activations` | `GET /api/eda/v1/activations/` | Read | Which rulebooks are active | P2 |
| 28 | `get_eda_rulebook_activation` | `GET /api/eda/v1/activations/{id}/` | Read | Activation detail, status, restart policy | P2 |
| 29 | `list_eda_decision_environments` | `GET /api/eda/v1/decision-environments/` | Read | Which DE runs this activation | P3 |
| 30 | `get_eda_decision_environment` | `GET /api/eda/v1/decision-environments/{id}/` | Read | DE image, org, credential | P3 |
| 31 | `sync_project` | `POST /api/v2/projects/{id}/update/` | **Write** | Trigger project SCM sync | P3 |
| 32 | `cancel_job` | `POST /api/v2/jobs/{id}/cancel/` | **Write** | Cancel a running/stuck job | P2 |
| 33 | `relaunch_job` | `POST /api/v2/jobs/{id}/relaunch/` | **Write** | Rerun a failed job | P2 |
| 34 | `toggle_eda_activation` | `POST /api/eda/v1/activations/{id}/enable\|disable/` | **Write** | Enable/disable a rulebook activation | P3 |

### 3.3 Tier 3 — MCP only (no local tools)

These remain MCP-only. Too admin-heavy, security-sensitive, or complex for local tools on mobile.

| Capability | Reason |
|---|---|
| Credentials CRUD (create/update/delete) | Security-sensitive, secret injection |
| Teams / Users / RBAC management | Admin activity, complex params |
| Notification templates | Setup task, not operational |
| Settings read/write | Dangerous from mobile |
| Bulk operations (bulk launch, bulk host create/delete) | Risky at scale |
| Export / Import | Admin migration |
| Ad-hoc commands | Complex params, future consideration |
| Workflow template node editing | Graph editing from mobile is bad UX |
| Workflow approvals (approve/deny) | Future Tier 2 candidate — approve pending deployments from phone |

## 4. Real-World Use Case Scenarios

### 4.1 "What jobs failed today?" (Read, local tool)

```
User: "What jobs failed today?"
LLM → tool_call: list_jobs(status="failed", ...)
ToolRouter → local tool: ListJobsLocalTool
  → JobRepository.getJobs(status="failed") → Retrofit → AAP API
  → ToolResult with job list
LLM → summarizes: "3 jobs failed today: Patching (host timeout), Deploy-prod (credential expired), Backup (disk full)"
```

**Why local wins:** Instant, no MCP server needed. Same data the Jobs screen shows.

### 4.2 "Launch the deploy-staging template" (Write, local tool + confirmation)

```
User: "Launch deploy-staging"
LLM → tool_call: launch_job(search="deploy-staging")
ToolRouter → local tool: LaunchJobLocalTool (destructive=true)
  Step 1: search templates → find ID 42
  Step 2: ChatEngine emits ConfirmationRequired("launch_job", {id: 42, name: "deploy-staging"})
  UI shows: "Launch job template 'deploy-staging'? [Approve] [Deny]"
  User taps Approve
  Step 3: TemplateRepository.launchJob(42) → Retrofit → AAP API
  → ToolResult with job ID
LLM → "Launched deploy-staging (Job #1847). I'll check the status in a moment."
```

**Why local wins:** The app already launches jobs from the Templates screen. The assistant uses the same code path.

### 4.3 "Is my cluster healthy?" (Read, new local tool — Tier 2)

```
User: "Is my cluster healthy?"
LLM → tool_call: ping() AND list_instances()
ToolRouter → local tools: PingLocalTool + ListInstancesLocalTool
  → AapApiService.ping() + AapApiService.getInstances()
  → ToolResults with cluster state + instance capacities
LLM → "Cluster is healthy. 3 control nodes, 2 execution nodes. All reporting OK.
        Execution node 'exec-02' is at 89% capacity — might want to watch that."
```

**Why local wins over MCP:** Health monitoring should work even if the MCP server is down. Ping is the most basic operation — no reason to route it through MCP.

### 4.4 "What credential does deploy-prod use?" (Read, new local tool — Tier 2)

```
User: "What credential does deploy-prod use?"
LLM → tool_call: list_job_templates(search="deploy-prod")
ToolRouter → local tool → gets template with credential ID
LLM → tool_call: get_credential(id=15)
ToolRouter → local tool → gets credential name, type, org
LLM → "deploy-prod uses 'Production SSH Key' (Machine credential, owned by Ops team)"
```

**Why local:** Two fast sequential calls vs. two MCP round-trips. The LLM chains them naturally.

### 4.5 "Show me EDA rulebook activations" (Read, new local tool — Tier 2)

```
User: "Which rulebooks are active?"
LLM → tool_call: list_eda_rulebook_activations()
ToolRouter → local tool → EdaApiService.getActivations()
LLM → "4 active rulebook activations:
        - remediate-disk-full (running, restart: on-failure)
        - alert-on-job-failure (running, restart: always)
        - auto-scale-hosts (disabled)
        - nightly-compliance-check (running, restart: never)"
```

### 4.6 "Create a new credential for the staging environment" (Write, MCP only)

```
User: "Create a Machine credential called 'Staging SSH' for the DevOps org"
LLM → tool_call: controller.credentials_create(name="Staging SSH", type="Machine", org="DevOps")
ToolRouter → MCP tool (no local equivalent for credential CRUD)
  → McpTool → MCP server → AAP API
  → ToolResult
LLM → "Created credential 'Staging SSH' (Machine type) in the DevOps organization."
```

**Why MCP:** Credential creation involves secret injection and complex params. Not worth building into the app — rare operation, admin-level.

### 4.7 "Show me the automation mesh topology" (Read, new local tool — Tier 2)

```
User: "Show me the mesh topology"
LLM → tool_call: get_topology()
ToolRouter → local tool → AapApiService.getMeshTopology()
LLM → "Automation mesh has 7 nodes:
        Control plane: ctrl-01, ctrl-02, ctrl-03 (all healthy)
        Execution: exec-01 (78% capacity), exec-02 (89% capacity)
        Hop: hop-east-01, hop-west-01
        Connections: ctrl-* → hop-east-01 → exec-01, ctrl-* → hop-west-01 → exec-02"
```

### 4.8 Mixed query — local + MCP in same turn

```
User: "What's the status of the patching job and who has permission to relaunch it?"
LLM → tool_call: list_jobs(search="patching") [LOCAL]
     + tool_call: controller.roles_list(resource_type="job_template", ...) [MCP]
ToolRouter → routes first to local, second to MCP
Both execute (potentially in parallel)
LLM → combines results from both sources
```

**This shows why MCP stays available:** RBAC queries are Tier 3, no local tool. Local and MCP tools coexist naturally.

## 5. Implementation Plan

### Phase 1: Core infrastructure + Tier 1 local tools

**Goal:** Local tools work for all 14 existing Retrofit endpoints.

1. Create `LocalTool` abstract class with `destructive` flag
2. Create `ToolSource` enum
3. Create `LocalToolRegistry` — Koin module that instantiates all local tools with repository injection
4. Evolve `ToolRouter` from static object to class with tool registration
5. Implement all 14 Tier 1 local tools
6. Add overlap mapping — disable MCP tools when local equivalent exists (default)
7. Add per-tool enable/disable to `ToolRouter`
8. Wire confirmation flow for write tools (#34)
9. Update `ChatEngine` to use evolved `ToolRouter`
10. Update system prompt — tell LLM about available local tools and their naming

### Phase 2: Tier 2 read-only endpoints (P1 + P2)

**Goal:** Health monitoring, credentials, projects, EDA activations accessible via local tools.

1. Add new endpoints to `AapApiService`:
   - `getInstances()`, `getInstance()`, `getInstanceGroups()`, `ping()`
   - `getMeshTopology()`
   - `getCredentials()`, `getCredential()`
   - `getProjects()`, `getProject()`
   - `getExecutionEnvironments()`

2. Add new endpoints to `EdaApiService`:
   - `getActivations()`, `getActivation()`
   - `getDecisionEnvironments()`, `getDecisionEnvironment()`

3. Create data classes for new response types
4. Create repository classes (or extend existing ones)
5. Create local tools for each new endpoint
6. Register in `LocalToolRegistry`

### Phase 3: Tier 2 write operations + P3 read-only

**Goal:** Cancel, relaunch, sync, toggle — all with confirmation flow.

1. Add write endpoints:
   - `cancelJob()`, `relaunchJob()`, `syncProject()`
   - `enableEdaActivation()`, `disableEdaActivation()`

2. Add remaining read-only endpoints (P3):
   - `getOrganizations()`, `getLabels()`

3. Create local tools with `destructive = true`
4. Implement "don't ask again" per-tool toggle (DataStore)

### Phase 4: MCP coexistence refinements

**Goal:** Smooth coexistence between local and MCP tools.

1. Per-tool enable/disable UI in assistant settings
2. Auto-detect overlap when MCP server connects (compare tool names against overlap mapping)
3. Combine with #52 toolset endpoints — configure MCP toolsets to exclude operations covered by local tools
4. Fallback: if local tool fails (e.g., API error), offer to retry via MCP

## 6. Data Models (Tier 2 new types)

### Controller

```kotlin
@Serializable
data class Instance(
    val id: Int,
    val hostname: String,
    @SerialName("node_type") val nodeType: String,  // "control", "execution", "hop", "hybrid"
    val enabled: Boolean,
    val capacity: Int,
    @SerialName("consumed_capacity") val consumedCapacity: Float,
    @SerialName("percent_capacity_remaining") val percentCapacityRemaining: Float,
    val errors: String,
    val version: String
)

@Serializable
data class InstanceGroup(
    val id: Int,
    val name: String,
    val instances: Int,  // count
    @SerialName("consumed_capacity") val consumedCapacity: Float,
    @SerialName("percent_capacity_remaining") val percentCapacityRemaining: Float,
    @SerialName("is_container_group") val isContainerGroup: Boolean
)

@Serializable
data class Credential(
    val id: Int,
    val name: String,
    val description: String,
    @SerialName("credential_type") val credentialType: Int,
    @SerialName("credential_type_name") val credentialTypeName: String? = null,
    val organization: Int? = null,
    @SerialName("organization_name") val organizationName: String? = null,
    val managed: Boolean = false
)

@Serializable
data class Project(
    val id: Int,
    val name: String,
    val description: String,
    @SerialName("scm_type") val scmType: String,
    @SerialName("scm_url") val scmUrl: String,
    @SerialName("scm_branch") val scmBranch: String,
    val status: String,  // "successful", "failed", "running", "pending", "never updated"
    @SerialName("last_job_run") val lastJobRun: String? = null,
    val organization: Int? = null
)

@Serializable
data class ExecutionEnvironment(
    val id: Int,
    val name: String,
    val description: String,
    val image: String,
    val organization: Int? = null,
    val managed: Boolean = false
)

@Serializable
data class PingResponse(
    val ha: Boolean,
    @SerialName("version") val version: String,
    @SerialName("active_node") val activeNode: String,
    @SerialName("install_uuid") val installUuid: String,
    val instances: List<PingInstance>
) {
    @Serializable
    data class PingInstance(
        val node: String,
        @SerialName("node_type") val nodeType: String,
        val capacity: Int,
        val version: String,
        val errors: String = ""
    )
}
```

### EDA

```kotlin
@Serializable
data class EdaRulebookActivation(
    val id: Int,
    val name: String,
    val description: String,
    val status: String,  // "running", "completed", "failed", "disabled"
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("restart_policy") val restartPolicy: String,
    @SerialName("rulebook_name") val rulebookName: String? = null,
    @SerialName("decision_environment_name") val decisionEnvironmentName: String? = null,
    @SerialName("project_name") val projectName: String? = null,
    @SerialName("organization_name") val organizationName: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class EdaDecisionEnvironment(
    val id: Int,
    val name: String,
    val description: String,
    val image: String,
    @SerialName("organization_name") val organizationName: String? = null,
    @SerialName("credential_name") val credentialName: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
```

## 7. Koin Module

```kotlin
// di/localToolsModule.kt
val localToolsModule = module {
    single { LocalToolRegistry(getAll()) }

    // Tier 1 — existing repositories
    factory { ListJobTemplatesLocalTool(get<TemplateRepository>()) }
    factory { LaunchJobLocalTool(get<TemplateRepository>()) }
    factory { GetJobLocalTool(get<JobRepository>()) }
    factory { GetJobStdoutLocalTool(get<JobRepository>()) }
    factory { ListJobsLocalTool(get<JobRepository>()) }
    factory { ListWorkflowTemplatesLocalTool(get<WorkflowRepository>()) }
    factory { LaunchWorkflowLocalTool(get<WorkflowRepository>()) }
    factory { GetWorkflowJobLocalTool(get<WorkflowRepository>()) }
    factory { ListInventoriesLocalTool(get<InventoryRepository>()) }
    factory { ListHostsLocalTool(get<HostRepository>()) }
    factory { GetHostFactsLocalTool(get<HostRepository>()) }
    factory { ListSchedulesLocalTool(get<ScheduleRepository>()) }
    factory { ToggleScheduleLocalTool(get<ScheduleRepository>()) }
    factory { ListEdaAuditRulesLocalTool(get<EdaAuditRepository>()) }

    // Tier 2 — new repositories (added in Phase 2/3)
    // factory { ListInstancesLocalTool(get<InfrastructureRepository>()) }
    // factory { PingLocalTool(get<InfrastructureRepository>()) }
    // ... etc
}
```

## 8. System Prompt Updates

Add to `ChatEngine.SYSTEM_PROMPT`:

```
You have access to local tools that connect directly to the AAP instance (fast, no MCP dependency)
and MCP tools that extend your capabilities beyond what the app supports natively.

Local tools are prefixed with simple names: list_jobs, launch_job, get_host_facts, etc.
MCP tools use the pattern: controller.resource_action, eda.resource_action.

Prefer local tools when available — they are faster and don't require an MCP server.
Use MCP tools only for operations that have no local equivalent.

For write operations (launch, cancel, toggle, sync), always explain what you're about to do
and wait for user confirmation before executing.
```

## 9. Token Impact

| Scenario | Current (MCP only) | With local tools |
|---|---|---|
| Tool schemas loaded | ~50 MCP tools × 1K chars = ~50K | 14 local (lean schemas ~300 chars each = ~4.2K) + reduced MCP set |
| "List my jobs" round-trip | App → MCP → AAP → MCP → App | App → AAP → App (1 fewer hop) |
| Tool result format | Opaque MCP JSON (verbose) | Typed Kotlin → controlled JSON (lean) |
| Combined with #52 toolsets | N/A | Local tools + focused MCP toolset = minimal token footprint |

## 10. Testing Strategy

### Unit tests

- Each `LocalTool.execute()` with mock repository
- `ToolRouter` overlap resolution (local preferred, MCP fallback)
- `ToolRouter` enable/disable per tool
- Write tool confirmation flag detection

### Integration tests

- `LocalTool` → real Retrofit call (with mock server / MockWebServer)
- `ToolRouter` with mixed local + MCP tools, verify correct routing
- End-to-end: `ChatEngine` with local tools, verify LLM receives local tool results

## 11. File Summary

### New files

| File | Purpose |
|---|---|
| `assistant/tools/LocalTool.kt` | Abstract base class with `destructive` flag |
| `assistant/tools/local/*.kt` | Individual local tool implementations (14 Tier 1 + 20 Tier 2) |
| `assistant/tools/LocalToolRegistry.kt` | Registers all local tools, provides to ToolRouter |
| `di/localToolsModule.kt` | Koin DI for local tools |
| `model/Instance.kt` | Data classes for Tier 2 controller types |
| `model/EdaRulebookActivation.kt` | Data classes for Tier 2 EDA types |
| `data/InfrastructureRepository.kt` | Repository for instances, ping, topology |
| `data/CredentialRepository.kt` | Repository for credentials (read-only) |
| `data/ProjectRepository.kt` | Repository for projects |

### Modified files

| File | Change |
|---|---|
| `assistant/engine/ToolRouter.kt` | Evolve from static object to class with registry |
| `assistant/engine/ChatEngine.kt` | Use new ToolRouter, add confirmation check for destructive tools |
| `assistant/presentation/AssistantViewModel.kt` | Initialize local tools, wire to ToolRouter |
| `network/AapApiService.kt` | Add Tier 2 endpoints |
| `network/EdaApiService.kt` | Add Tier 2 EDA endpoints |
| `di/networkModule.kt` | Add new repositories |

## 12. Open Questions

1. **Topology response format:** Need to verify the exact response shape of `GET /api/v2/mesh_visualizer/` — it may return a graph structure (nodes + edges) that needs a custom data class.
2. **Credential type resolution:** `GET /api/v2/credentials/{id}/` returns a `credential_type` integer ID. Should we fetch the type name in the same tool call (extra request) or let the LLM ask separately?
3. **EDA activation enable/disable:** The API may use `POST .../enable/` and `POST .../disable/` as separate endpoints, or `PATCH` with `is_enabled` field. Need to verify against AAP 2.5+ API.
4. **Local tool schema size:** Should local tool JSON schemas be minimal (just parameter names + types) or match the MCP schema verbosity? Minimal saves tokens but gives the LLM less context.
