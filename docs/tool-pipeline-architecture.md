# Tool Pipeline Architecture

How user messages flow through the tool system — from query to LLM response.

## Components

| Component | Location | Responsibility |
|-----------|----------|---------------|
| **AssistantViewModel** | `assistant/presentation/AssistantViewModel.kt` | Orchestrates the pipeline: creates router, budgets tools, starts engine |
| **ToolRouter** | `assistant/engine/ToolRouter.kt` | Selects relevant tools per query via category matching + scoring |
| **ChatEngine** | `assistant/engine/ChatEngine.kt` | Agentic loop: sends tools + message to LLM, executes tool calls, repeats |
| **ToolExecutor** | `assistant/engine/ToolExecutor.kt` | Executes individual tool calls with timeout, caching, and result truncation |
| **LocalTool** | `assistant/tools/local/*.kt` | 26 tools calling AAP APIs directly via Retrofit |
| **McpTool** | `assistant/tools/McpTool.kt` | Wraps MCP server tools as the `Tool` interface |
| **McpClient** | `network/mcp/McpClient.kt` | JSON-RPC 2.0 + SSE connection to AAP MCP servers |
| **McpServerManager** | `network/mcp/McpServerManager.kt` | Multi-server connection lifecycle, tool discovery |

## Pipeline Flow

```
User sends message
    │
    ▼
┌─────────────────────────────────────────────────┐
│ AssistantViewModel.sendMessage()                │
│                                                 │
│  1. Create ToolRouter                           │
│  2. Register local tools (26)                   │
│     (auto-disables overlapping MCP tools via     │
│      OVERLAP_MAPPING during registration)       │
│  3. Register MCP tools (from McpServerManager)  │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│ ToolRouter.getToolsForQuery(message)            │
│                                                 │
│  1. Parse: lowercase → split → remove stopwords │
│     → stem                                      │
│  2. Match categories (7): stemmed query words    │
│     vs category keyword sets                    │
│  3. Filter local tools: name in matched         │
│     category + enabled                          │
│  4. Filter MCP tools: resource prefix match     │
│     + enabled + read-only enforcement           │
│  5. cherryPick(): score by name-part overlap    │
│     with query, boost list/ping/get, penalize   │
│     destructive                                 │
│  6. Return QueryResult(tools, categoryMatched)  │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│ AssistantViewModel (continued)                  │
│                                                 │
│  1. Early return: if no category matched or      │
│     no tools after filtering, show guidance     │
│     message and return (BUG: bypasses fallback) │
│  2. Cap MCP tools by TokenSavingMode:           │
│     STANDARD=10, TOKEN_SAVER=5, TOOLS_ONLY=3    │
│  3. Fallback: if both matchedLocal and          │
│     matchedMcp empty, add ListToolsLocalTool    │
│  4. Set contextChars by mode:                   │
│     STANDARD=16K, TOKEN_SAVER=8K, TOOLS_ONLY=4K │
│  5. Create ToolExecutor(allRegisteredTools)      │
│  6. Create ChatEngine(llmProvider, toolExecutor) │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│ ChatEngine.processMessage(message, history,     │
│                           toolSpecs)            │
│                                                 │
│  1. Convert ToolSpec → Koog ToolDescriptor      │
│     (via ToolDescriptorMapping)                 │
│  2. Build system prompt with tool guidance      │
│  3. Send to LLM provider (streaming)            │
│                                                 │
│  ┌──── Agentic loop (max 10 iterations) ──────┐ │
│  │                                             │ │
│  │  LLM response contains tool call?           │ │
│  │  ├─ YES → ToolExecutor.execute(call)        │ │
│  │  │         ├─ Find tool by name             │ │
│  │  │         ├─ Check 2-min result cache      │ │
│  │  │         ├─ Execute with 30s timeout      │ │
│  │  │         ├─ capResultArray (max 10 items) │ │
│  │  │         ├─ smartTruncate (8K char limit) │ │
│  │  │         └─ Return result to LLM          │ │
│  │  │         → loop back                      │ │
│  │  │                                          │ │
│  │  ├─ NO (text response) → emit to UI, done   │ │
│  │  │                                          │ │
│  │  └─ Repeat detected → break                 │ │
│  └─────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

## Tool Sources

### Local Tools (26)

Call AAP APIs directly via Retrofit. Zero latency to MCP server. Defined in `assistant/tools/local/`.

Examples: `list_job_templates`, `launch_job`, `get_host_facts`, `ping`, `list_eda_activations`

Each implements `Tool` interface:
```kotlin
interface Tool {
    val spec: ToolSpec  // name, description, parametersSchema
    suspend fun execute(args: JsonObject): ToolResult
}
```

### MCP Tools

Discovered dynamically from connected MCP servers via `McpClient`. Wrapped as `McpTool` implementing the same `Tool` interface. Server label embedded in description: `"[AAP Controller] List job templates"` (truncated to 120 chars).

Discovery flow:
```
McpServerManager.connectAll()
    → McpClient.connect(url)    // SSE + JSON-RPC initialize handshake
    → McpClient.listTools()     // tools/list request
    → McpTool(definition)       // wrap each as Tool interface
    → Register in ToolRouter
```

## ToolRouter Details

### Categories (7)

| Category | Keywords (sample) | Local Tools (sample) |
|----------|------------------|---------------------|
| INVENTORY | host, inventory, group, variable | list_inventories, list_hosts, get_host_facts |
| JOBS | job, template, launch, run, playbook | list_job_templates, launch_job, get_job |
| MONITORING | status, health, instance, node, mesh | list_instances, get_mesh_topology, ping |
| USERS | user, team, role, permission, access | (MCP only) |
| SECURITY | credential, vault, secret, token | list_credentials, get_credential |
| CONFIGURATION | project, execution, environment, setting | list_execution_environments, list_projects |
| EDA | eda, rulebook, activation, event, webhook | list_eda_activations, list_eda_audit_rules |

### Overlap Prevention

When a local tool exists, its MCP equivalent is auto-disabled:
```
list_job_templates → controller.job_templates_list (disabled)
launch_job → controller.job_templates_launch_create (disabled)
... (26 local tool keys, 27 MCP tool names — toggle_schedule maps to 2)
```

### Read-Only Enforcement

MCP servers with `readOnly: true` in config have write tools filtered out. Detection by tool name suffix: `_create`, `_update`, `_delete`, `_launch`, `_relaunch`, `_cancel`.

## Token Optimization

Three tiers control schema verbosity and tool count:

| TokenSavingMode | Description | MCP Tool Cap | Context Chars |
|-----------------|------------|-------------|--------------|
| STANDARD | Full conversation with LLM, tools when relevant | 10 | 16,000 |
| TOKEN_SAVER | Short replies for general chat, tools when relevant | 5 | 8,000 |
| TOOLS_ONLY | Tools only — no general conversation | 3 | 4,000 |

Schema compaction handled by `ToolDescriptorMapping.compactSchema()`.

## Known Issues

- **Fallback bug** (`AssistantViewModel.kt:149-151`): when no category matches or no tools pass filtering, an early return shows a guidance message and bypasses the `ListToolsLocalTool` fallback. `ToolRouter.kt:197-200` correctly returns `categoryMatched = false`, but AssistantViewModel's early-return logic prevents the fallback from ever being reached
- **Per-tool enable/disable not persisted**: state in `disabledTools` set lost on ViewModel recreation
- **Stemmer too simplistic**: only handles plurals (-s/-es/-ies), misses -ing/-ed/-er

## Planned Improvements

See issue #120 for the 5-phase improvement plan covering semantic search (Model2Vec), model capability tiers, AAP role filtering, and token optimization. See issue #30 for local LLM backend integration.
