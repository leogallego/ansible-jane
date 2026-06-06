# Full Code Review - 2026-05-25

This report consolidates a full-repo review split across five parallel tracks (network/security/DI, data/model, UI/presentation, assistant engine, tools/MCP), followed by an end-to-end validation pass.

## Scope

- `app/src/main/kotlin/io/github/leogallego/ansiblejane/**`
- Focus: correctness, security, behavior regressions, reliability, lifecycle/concurrency, and missing high-risk tests.
- Excludes style-only feedback.

## Findings

### Critical

1) **Sensitive request data is logged at BODY level in production paths**
- **Evidence:** `network/AapApiProvider.kt`, `network/NetworkModule.kt`, `data/AuthRepository.kt`, `assistant/AssistantModule.kt`
- **Why it matters:** `HttpLoggingInterceptor.Level.BODY` is enabled on multiple clients that carry bearer tokens and request/response payloads (AAP, MCP, LLM).
- **Impact:** Tokens and potentially sensitive response bodies can leak through logs.
- **Fix direction:** Gate BODY logging behind debug builds only, disable or redact `Authorization` in release.

2) **AAP bearer token is sent to every MCP server URL**
- **Evidence:** `assistant/AssistantModule.kt`, `network/mcp/McpServerManager.kt`, `model/AapInstance.kt`
- **Why it matters:** MCP clients are always built with `AuthInterceptor(tokenProvider = { instance.token })`.
- **Impact:** If a user adds a non-AAP MCP endpoint, the AAP token is still attached to that server.
- **Fix direction:** Honor `McpServerConfig.useInstanceAuth`; use separate auth/no-auth clients per server config.

### High

3) **LLM API keys are persisted unencrypted**
- **Evidence:** `assistant/data/AssistantRepository.kt`, `assistant/data/AssistantConfig.kt`
- **Why it matters:** LLM provider configs (including `apiKey`) are serialized directly into `assistant_config` DataStore.
- **Impact:** Credential exposure risk if local app data is extracted.
- **Fix direction:** Encrypt LLM secrets using the same Tink/Keystore pattern used by `TokenManager`.

4) **Re-auth/update path wipes MCP settings and discovered instance metadata**
- **Evidence:** `data/TokenManager.kt` (`saveInstance` with `existingId`)
- **Why it matters:** Updating existing instances replaces the whole serialized record with a new `AapInstance` that only carries auth/base fields.
- **Impact:** Re-auth can silently clear `mcpServerUrls`, `mcpEnabled`, and `instanceInfo`.
- **Fix direction:** Merge credentials into the existing serialized record instead of full replacement.

5) **MCP write operations bypass confirmation guard**
- **Evidence:** `assistant/engine/ChatEngine.kt`, `assistant/tools/LocalTool.kt`, `assistant/tools/McpTool.kt`
- **Why it matters:** Confirmation is only enforced for `LocalTool` with `destructive = true`.
- **Impact:** Equivalent MCP writes can execute without user confirmation.
- **Fix direction:** Add a unified write-action confirmation policy for local + MCP tools.

6) **MCP write operations are cached as if they were read-only**
- **Evidence:** `assistant/engine/ToolExecutor.kt`
- **Why it matters:** Cache bypass only checks `tool is LocalTool && destructive`; MCP tools are cacheable even for write actions.
- **Impact:** Repeated write calls can return cached success and skip real execution.
- **Fix direction:** Mark MCP write tools as non-cacheable (suffix policy or explicit flag).

7) **Read-only MCP filter misses `_partial_update`**
- **Evidence:** `assistant/engine/ToolRouter.kt`
- **Why it matters:** `WRITE_ACTIONS` omits `_partial_update`.
- **Impact:** `controller.schedules_partial_update` can pass read-only filtering.
- **Fix direction:** Expand write suffix rules and add tests for real AAP MCP write names.

8) **Approval polling marks approvals as seen even when notification was not posted**
- **Evidence:** `notification/ApprovalPollingWorker.kt`, `notification/ApprovalNotificationManager.kt`
- **Why it matters:** `showNotification()` returns early when notification permission is denied, but `markSeen(newIds)` still runs.
- **Impact:** Users can permanently miss approval alerts.
- **Fix direction:** Mark IDs as seen only when notification delivery succeeds.

9) **Backup REPLACE import is non-transactional and can wipe credentials**
- **Evidence:** `presentation/settings/BackupViewModel.kt`
- **Why it matters:** `clearCredentials()` is called before import completes; failures are not rolled back.
- **Impact:** Partial import failure can leave users with no credentials.
- **Fix direction:** Stage import first and atomically swap on full success.

10) **Assistant prompt history is duplicated**
- **Evidence:** `assistant/presentation/AssistantViewModel.kt`, `assistant/engine/ChatEngine.kt`
- **Why it matters:** `sendMessage()` stores the user message, then `processMessage()` receives full history and appends the same user text again.
- **Impact:** Higher token usage and reduced response quality.
- **Fix direction:** Keep one source of truth (either history append in VM or message append in engine, not both).

11) **Regenerate flow multiplies duplicate user turns**
- **Evidence:** `assistant/presentation/AssistantViewModel.kt`
- **Why it matters:** `regenerateLastMessage()` removes the last assistant message but calls `sendMessage(lastUserMsg.content)`, re-adding the user message.
- **Impact:** Conversation history drifts and quality degrades across retries.
- **Fix direction:** Regenerate without re-adding the same user turn.

### Medium

12) **Workflow node retrieval returns first page only**
- **Evidence:** `data/WorkflowRepository.kt`, `network/AapApiService.kt`
- **Why it matters:** `getWorkflowNodes()` and `getWorkflowTemplateNodes()` do not iterate pagination.
- **Impact:** Large workflow graphs can be truncated.
- **Fix direction:** Add paginated fetch helpers and aggregate until `next == null`.

13) **Disabling MCP clears all server configuration**
- **Evidence:** `presentation/settings/SettingsViewModel.kt`, `data/TokenManager.kt`
- **Why it matters:** `toggleMcpEnabled(false)` writes `servers = null`.
- **Impact:** Re-enabling MCP loses custom/manual setup and toggles.
- **Fix direction:** Preserve server list when disabling; only flip the enable flag.

14) **Dashboard loads can race across refresh/instance switches**
- **Evidence:** `presentation/dashboard/DashboardViewModel.kt`
- **Why it matters:** `loadDashboard()` launches parallel calls and commits whichever completes last, without canceling prior runs or pinning an instance snapshot.
- **Impact:** Stale/mixed dashboard state under fast refresh or instance change.
- **Fix direction:** Cancel in-flight dashboard jobs before starting new ones and discard mismatched instance results.

15) **Credential check conflates missing credentials with transient failures**
- **Evidence:** `data/AuthRepository.kt`
- **Why it matters:** `checkExistingCredentials()` returns `null` both for "no credentials" and "validation/network failure".
- **Impact:** UI cannot distinguish offline/invalid session from logged-out state.
- **Fix direction:** Return a sealed result type (`NoCredentials`, `Valid`, `ValidationFailed`).

16) **Malformed tool arguments silently degrade to `{}` and still execute**
- **Evidence:** `assistant/engine/ToolExecutor.kt`
- **Why it matters:** JSON parse errors default to empty args object.
- **Impact:** Risk of unintended default behavior or confusing failures.
- **Fix direction:** Return a validation error instead of executing with empty args.

## Important Test Gaps

- No direct unit tests for real `TokenManager` mutation behavior (merge/update/race/corruption paths).
- Missing tests for assistant write safety: MCP confirmation, MCP cache policy, malformed arg handling.
- Missing tests for `ApprovalPollingWorker` permission-denied flow.
- Missing tests for dashboard refresh race/instance switch consistency.
- Missing pagination tests for workflow node retrieval.

## Stable Areas Noted

- Koin module separation and repository boundaries are generally clean and consistent.
- Assistant streaming parsing and provider-level error mapping have meaningful test coverage.
- Backup cryptography primitives and parameter choices are reasonable.
- Compose screens show good test-tag coverage for primary interactive controls.

## Suggested Prioritization

1. Security hardening first: logging, token forwarding to MCP, encrypted LLM keys.
2. Assistant write-safety fixes: confirmation + cache behavior + read-only filtering.
3. Data-loss and notification reliability: backup replace transactionality and approval seen-tracking.
4. UX/reliability follow-ups: duplicate chat history, workflow pagination, dashboard race handling.
