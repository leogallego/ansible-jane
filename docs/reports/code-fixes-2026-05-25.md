# Code Fixes Assessment - 2026-05-25

QA/Security verification of the code review report from `code-review-2026-05-25.md`. Every finding was traced against source code. All 16 findings are **CONFIRMED**.

---

## Verification Summary

| # | Finding | Verdict | Severity | Category |
|---|---------|---------|----------|----------|
| 1 | BODY-level logging in production | CONFIRMED | Critical | Security |
| 2 | AAP token sent to all MCP servers | CONFIRMED | Critical | Security |
| 3 | LLM API keys stored unencrypted | CONFIRMED | High | Security |
| 4 | Re-auth wipes MCP settings | CONFIRMED | High | Data Integrity |
| 5 | MCP writes bypass confirmation | CONFIRMED | High | Safety |
| 6 | MCP writes cached as read-only | CONFIRMED | High | Safety |
| 7 | Read-only filter misses _partial_update | CONFIRMED | High | Safety |
| 8 | Approvals marked seen without notification | CONFIRMED | High | Reliability |
| 9 | Backup REPLACE is non-transactional | CONFIRMED | High | Data Integrity |
| 10 | Chat history duplicates user messages | CONFIRMED | High | Correctness |
| 11 | Regenerate compounds duplicate turns | CONFIRMED | High | Correctness |
| 12 | Workflow nodes not paginated | CONFIRMED | Medium | Completeness |
| 13 | Disabling MCP clears server config | CONFIRMED | High | Data Integrity |
| 14 | Dashboard races on refresh/instance switch | CONFIRMED | Medium | Reliability |
| 15 | Credential check conflates missing vs failed | CONFIRMED | Medium | UX |
| 16 | Malformed tool args silently degrade to {} | CONFIRMED | High | Safety |

---

## Detailed Fix Assessments

### 1. BODY-level logging in production (CRITICAL)

**Files:** `network/AapApiProvider.kt:100-102`, `network/NetworkModule.kt:27-29`, `data/AuthRepository.kt:155-157`, `assistant/AssistantModule.kt:30-32`

**Verified:** All four OkHttpClient builders unconditionally set `HttpLoggingInterceptor.Level.BODY`. This logs full request/response bodies including `Authorization: Bearer <token>` headers and API payloads in all build variants.

**Fix direction:**
- Gate logging level behind `BuildConfig.DEBUG`:
  ```kotlin
  .addInterceptor(HttpLoggingInterceptor().apply {
      level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
              else HttpLoggingInterceptor.Level.NONE
  })
  ```
- Consider a custom `Interceptor` that redacts `Authorization` headers even in debug builds.
- Apply consistently across all four client builders.

**Risk if unfixed:** Bearer tokens and API keys are visible in logcat on any device. Any app with `READ_LOGS` permission or ADB access can extract credentials.

---

### 2. AAP bearer token sent to all MCP servers (CRITICAL)

**Files:** `assistant/AssistantModule.kt:22-42`, `network/mcp/McpServerManager.kt:25-31`, `network/AuthInterceptor.kt:21-30`

**Verified:** `McpServerManager` receives a single `httpClientFactory` that always attaches `AuthInterceptor(tokenProvider = { instance.token })`. The `McpServerConfig.useInstanceAuth` field exists in the model (`AapInstance.kt`) but is never checked anywhere in the codebase (confirmed via grep).

**Fix direction:**
- In `McpServerManager.connectAll()`, check `config.useInstanceAuth` before attaching the auth interceptor.
- Create two client variants: one with `AuthInterceptor`, one without. Select per `McpServerConfig`.
- For servers with custom auth, support a per-server token/header field in `McpServerConfig`.

**Risk if unfixed:** Users who add third-party MCP servers (e.g., a community tool server) will leak their AAP bearer token to that server. This is a credential exfiltration vector.

---

### 3. LLM API keys stored unencrypted (HIGH)

**Files:** `assistant/data/AssistantRepository.kt:24-26,61-75`, `assistant/data/AssistantConfig.kt:31-41`

**Verified:** `LlmProviderConfig.OpenAiCompatible.apiKey` is serialized as plain JSON into `assistant_config` Preferences DataStore. No encryption is applied. The app already has Tink/Keystore encryption infrastructure in `TokenManager`.

**Fix direction:**
- Encrypt the `apiKey` field using the existing Tink `Aead` primitive from `TokenManager` before writing to DataStore.
- Decrypt on read. Store encrypted bytes as Base64 in the JSON field.
- Alternative: store LLM keys in the same encrypted DataStore used by `TokenManager` rather than the plain `assistant_config` store.

**Risk if unfixed:** On rooted devices or via ADB backup, LLM API keys (OpenAI, OpenRouter, Gemini) can be extracted from app data. These keys may have billing implications.

---

### 4. Re-auth/update path wipes MCP settings (HIGH)

**Files:** `data/TokenManager.kt:171-243`

**Verified:** When `saveInstance(existingId = ...)` is called during re-authentication, lines 214-222 create a new `AapInstance` without MCP fields (`mcpServerUrls`, `mcpEnabled`, `instanceInfo` default to null/false). This temporary object is passed to `toSerialized()` at line 227, which faithfully copies the null values, replacing the existing serialized record with one missing MCP config.

**Fix direction:**
- When `existingId != null`, read the existing `SerializedInstance` first and merge only the auth-related fields:
  ```kotlin
  val existing = state.instances.first { it.id == existingId }
  val updated = existing.copy(
      encryptedToken = encrypt(token),
      alias = alias?.ifBlank { null } ?: existing.alias,
      apiVersion = apiVersion.name,
      trustSelfSigned = trustSelfSigned,
      certFingerprint = certFingerprint
  )
  ```
- This preserves `mcpServerUrls`, `mcpEnabled`, and `instanceInfo`.

**Risk if unfixed:** Users who re-authenticate lose all MCP server configuration and discovered instance metadata. Silent data loss.

---

### 5. MCP write operations bypass confirmation guard (HIGH)

**Files:** `assistant/engine/ChatEngine.kt:180-194`, `assistant/tools/McpTool.kt`

**Verified:** The confirmation gate at line 180 checks `tool is LocalTool && tool.destructive`. Since `McpTool` implements `Tool` (not `LocalTool`), it never triggers confirmation. MCP tools with write semantics (`_create`, `_update`, `_delete`, `_launch`) execute without user approval.

**Fix direction:**
- Add a `destructive` property to the `Tool` interface (or `McpTool` specifically).
- Infer destructiveness from MCP tool name suffixes using the same `WRITE_ACTIONS` set from `ToolRouter`.
- Update the ChatEngine confirmation gate to check `tool.destructive` regardless of tool type:
  ```kotlin
  val toolResult = if (tool.destructive && onConfirmationRequired != null) {
  ```

**Risk if unfixed:** The AI assistant can execute destructive AAP operations (delete inventories, cancel jobs, modify schedules) via MCP tools without user confirmation.

---

### 6. MCP write operations cached as read-only (HIGH)

**Files:** `assistant/engine/ToolExecutor.kt:39-46,88-90`

**Verified:** Cache bypass at line 39 checks `tool is LocalTool && tool.destructive`. MCP tools always evaluate to `isDestructive = false`, so write operations are cached for `CACHE_TTL_MS` (2 minutes). A second identical write call within that window returns the cached result instead of executing.

**Fix direction:**
- Unify destructiveness detection across tool types:
  ```kotlin
  val isDestructive = when (tool) {
      is LocalTool -> tool.destructive
      is McpTool -> WRITE_ACTIONS.any { tool.spec.name.endsWith(it) }
      else -> false
  }
  ```
- Or add `destructive` to the `Tool` interface and set it during MCP tool construction.

**Risk if unfixed:** Repeated MCP write calls (e.g., "launch that job again") may silently return cached success without executing. Users believe the action happened twice but it only ran once.

---

### 7. Read-only MCP filter misses _partial_update (HIGH)

**Files:** `assistant/engine/ToolRouter.kt:207-210,313-324`

**Verified:** `WRITE_ACTIONS` contains: `_create`, `_update`, `_delete`, `_launch`, `_relaunch`, `_cancel`. It does NOT contain `_partial_update`. The AAP MCP server exposes tools like `controller.schedules_partial_update` (confirmed in ToolRouter.kt line 153 mapping). These pass the read-only filter because `endsWith("_partial_update")` does not match any entry in `WRITE_ACTIONS`.

**Fix direction:**
- Add `_partial_update` to `WRITE_ACTIONS`:
  ```kotlin
  private val WRITE_ACTIONS = setOf(
      "_create", "_update", "_delete",
      "_launch", "_relaunch", "_cancel",
      "_partial_update"
  )
  ```
- Also audit for other AAP MCP write suffixes that may be missing (e.g., `_approve`, `_deny`, `_copy`, `_sync`).
- Add tests that enumerate real AAP MCP tool names against the filter.

**Risk if unfixed:** Read-only MCP servers can still execute partial updates, violating the read-only contract.

---

### 8. Approval polling marks seen before notification posts (HIGH)

**Files:** `notification/ApprovalPollingWorker.kt:45-53`, `notification/ApprovalNotificationManager.kt:39-46`

**Verified:** `ApprovalNotificationManager.showNotification()` returns early (no-op) when `POST_NOTIFICATIONS` permission is denied on Android 13+. Back in the worker, `approvalTracker.markSeen(newIds)` runs unconditionally after the loop completes, regardless of whether any notification was actually posted.

**Fix direction:**
- Make `showNotification()` return a `Boolean` indicating success.
- Only add IDs to the "seen" set when notification was actually delivered:
  ```kotlin
  val delivered = mutableListOf<Int>()
  for (approval in newApprovals) {
      if (notificationManager.showNotification(context, approval)) {
          delivered.add(approval.id)
      }
  }
  if (delivered.isNotEmpty()) approvalTracker.markSeen(delivered)
  ```

**Risk if unfixed:** Users who haven't granted notification permission permanently miss workflow approval alerts with no way to see them later.

---

### 9. Backup REPLACE is non-transactional (HIGH)

**Files:** `presentation/settings/BackupViewModel.kt:104-160`

**Verified:** In `confirmImport()` with `ImportMode.REPLACE`, line 105 calls `tokenManager.clearCredentials()` immediately. Lines 113-153 then iterate through instances and LLM configs to restore them. Any exception during the restore loop (network error, invalid API version, write failure) lands in the catch block at line 158 which only sets an error state -- no rollback.

**Fix direction:**
- Stage-then-swap approach:
  1. Parse and validate all import data first (without writing).
  2. Save the current state as a rollback snapshot.
  3. Clear and write new data.
  4. On failure, restore from the rollback snapshot.
- Or: write imported data to a temporary store, validate, then atomically replace the primary store.

**Risk if unfixed:** A partial restore failure leaves the user with no credentials and no way to recover without manually re-entering all instance configurations.

---

### 10. Chat history duplicates user messages (HIGH)

**Files:** `assistant/presentation/AssistantViewModel.kt:130-131,225-226`, `assistant/engine/ChatEngine.kt:62-64`

**Verified:** The duplication path:
1. `AssistantViewModel.sendMessage()` line 131: `repository.addMessage(userMessage)` -- adds user message to history.
2. Line 225-226: calls `engine.processMessage(text, repository.getHistory(), ...)` -- passes both the raw text AND the history that already contains that text.
3. `ChatEngine.processMessage()` line 62-63: iterates history and adds all messages including the user message.
4. Line 64: `messages.add(ChatMessage(role = Role.USER, content = userMessage))` -- adds the same user message again.

Result: the user message appears twice in the LLM context on every turn.

**Fix direction:**
- Remove the explicit `messages.add()` at ChatEngine line 64 since the history already contains the user message.
- Or: don't add to repository before calling processMessage, and let the engine be the sole source of message appending.
- Choose one source of truth for user message insertion.

**Risk if unfixed:** Higher token consumption (doubled user messages), potential LLM confusion from seeing the same message twice, reduced response quality.

---

### 11. Regenerate compounds duplicate user turns (HIGH)

**Files:** `assistant/presentation/AssistantViewModel.kt:312-316`

**Verified:** `regenerateLastMessage()` at line 314 removes only the last assistant message. Line 316 calls `sendMessage(lastUserMsg.content)`, which at line 131 adds the user message to repository again. The original user message was NOT removed, so the history now has two copies. Each regeneration adds another duplicate.

**Fix direction:**
- Option A: `regenerateLastMessage()` should remove both the last assistant message AND the last user message, then call `sendMessage()` normally.
- Option B: Add a flag to `sendMessage()` to skip the `repository.addMessage()` call when regenerating:
  ```kotlin
  fun sendMessage(text: String, isRegenerate: Boolean = false) {
      if (!isRegenerate) repository.addMessage(userMessage)
      ...
  }
  ```

**Risk if unfixed:** Each regeneration pollutes the conversation history with duplicate user turns, degrading response quality and wasting tokens. After 3 regenerations, the user message appears 4 times.

---

### 12. Workflow node retrieval returns first page only (MEDIUM)

**Files:** `data/WorkflowRepository.kt:70-89`, `network/AapApiService.kt:82-86`

**Verified:** Both `getWorkflowNodes()` and `getWorkflowTemplateNodes()` make a single API call with `pageSize=200` and return `response.results` without checking `response.next`. The `PaginatedResponse` model has a `next` field but it's never consumed.

**Fix direction:**
- Add a paginated fetch helper:
  ```kotlin
  private suspend fun <T> fetchAllPages(
      fetcher: suspend (page: Int) -> PaginatedResponse<T>
  ): List<T> {
      val all = mutableListOf<T>()
      var page = 1
      do {
          val response = fetcher(page)
          all.addAll(response.results)
          page++
      } while (response.next != null)
      return all
  }
  ```
- Apply to both workflow node methods.

**Risk if unfixed:** Workflow visualizations and status tracking silently truncate for workflows with >200 nodes. Users see incomplete workflow graphs with no indication of missing data.

---

### 13. Disabling MCP clears server configuration (HIGH)

**Files:** `presentation/settings/SettingsViewModel.kt:250-256`, `data/TokenManager.kt:348-360`

**Verified:** `toggleMcpEnabled(false)` at line 254 calls `tokenManager.updateMcpConfig(instance.id, false, null)`. `TokenManager.updateMcpConfig()` at line 357 writes `mcpServerUrls = servers` (which is `null`), destroying the server list.

**Fix direction:**
- When disabling, only flip the `mcpEnabled` flag; preserve the server list:
  ```kotlin
  if (!enabled) {
      tokenManager.updateMcpConfig(instance.id, false, instance.mcpServerUrls)
      return@launch
  }
  ```
- Or: add a separate `setMcpEnabled(instanceId, enabled)` method that only touches the boolean.

**Risk if unfixed:** Users who toggle MCP off and back on lose all manually-configured MCP servers and must reconfigure them.

---

### 14. Dashboard races on refresh/instance switch (MEDIUM)

**Files:** `presentation/dashboard/DashboardViewModel.kt:42-48,65-67,69-177`

**Verified:** `loadDashboard()` launches parallel `async` jobs in `viewModelScope` without canceling prior invocations. Instance is read at line 156 after all jobs complete, not at launch time. No guard prevents concurrent `loadDashboard()` calls from overlapping.

**Fix direction:**
- Track the loading job and cancel on new invocation:
  ```kotlin
  private var dashboardJob: Job? = null
  
  private fun loadDashboard() {
      dashboardJob?.cancel()
      dashboardJob = viewModelScope.launch {
          val instance = tokenManager.activeInstance.value ?: return@launch
          // ... use this pinned instance reference throughout
      }
  }
  ```
- Pin the instance at the start of `loadDashboard()`, not at line 156.

**Risk if unfixed:** Fast instance switches or rapid refresh taps can show data from the wrong instance on the dashboard. Low frequency but confusing when it happens.

---

### 15. Credential check conflates missing vs failed (MEDIUM)

**Files:** `data/AuthRepository.kt:104-123`

**Verified:** `checkExistingCredentials()` returns `null` both when `tokenManager.loadCredentials()` returns false (no stored credentials, line 105) and when the API validation call throws an exception (line 120). The caller cannot distinguish between these states.

**Fix direction:**
- Return a sealed result type:
  ```kotlin
  sealed class CredentialStatus {
      data class Valid(val user: User) : CredentialStatus()
      object NoCredentials : CredentialStatus()
      data class ValidationFailed(val error: Throwable) : CredentialStatus()
  }
  ```
- Update `checkExistingCredentials()` to return `CredentialStatus`.
- Update callers to handle each case appropriately (show login screen vs retry/offline banner).

**Risk if unfixed:** Users experiencing temporary network issues are treated as logged out, potentially losing their session context. The app cannot show "offline mode" or "retry" UI.

---

### 16. Malformed tool args silently degrade to {} (HIGH)

**Files:** `assistant/engine/ToolExecutor.kt:49-54,58-61`

**Verified:** JSON parse errors at line 53 are caught and silently replaced with `JsonObject(emptyMap())`. The tool then executes at line 60 with these empty args. No error is returned to the LLM to correct its tool call.

**Fix direction:**
- Return a validation error instead of executing:
  ```kotlin
  val argsJson = try {
      val parsed = json.parseToJsonElement(toolCall.args)
      if (parsed is JsonObject) parsed else JsonObject(emptyMap())
  } catch (e: Exception) {
      return ToolResult(
          success = false,
          data = "Invalid tool arguments: ${e.message}"
      )
  }
  ```
- This gives the LLM a chance to correct its tool call format.

**Risk if unfixed:** Tools execute with missing required parameters, potentially causing confusing error messages or unintended default behavior. The LLM never learns its formatting was wrong.

---

## Test Gaps Verification

All test gaps listed in the original report are confirmed:

| Gap | Status |
|-----|--------|
| TokenManager mutation tests (merge/update/race/corruption) | No tests found |
| MCP confirmation policy tests | No tests found |
| MCP cache policy tests | No tests found |
| Malformed arg handling tests | No tests found |
| ApprovalPollingWorker permission-denied flow tests | No tests found |
| Dashboard refresh race/instance switch tests | No tests found |
| Workflow node pagination tests | No tests found |

---

## Implementation Decisions

Decisions made during review session (2026-05-25):

| # | Finding | Decision | Rationale |
|---|---------|----------|-----------|
| 2 | Token leakage to MCP | **Option A: honor `useInstanceAuth` flag only.** No per-server credential fields yet. Future follow-up: Kai-style generic `headers: Map<String, String>` per server as part of tools UI overhaul. | Minimal fix covers current use cases (AAP auth + unauthenticated servers like ansible-know-mcp). |
| 3 | Unencrypted LLM keys | **Centralize in TokenManager.** Move LLM keys into the existing Tink-encrypted store. AssistantRepository keeps non-sensitive config only (model, URL, provider type, token saving mode). | Best practice: one `Aead` instance, one secrets store, multiple domain repos delegate. Easier to audit. |
| 5/6 | MCP destructive detection | **Add `destructive` to `Tool` interface.** `McpTool` computes it at construction from `WRITE_ACTIONS` suffixes. ChatEngine and ToolExecutor both check `tool.destructive` with no type checks. | Property belongs on the tool, not the caller. Avoids duplicated suffix logic. Enables future UI indicators on destructive tools. |
| 9 | Backup REPLACE safety | **Validate-first, no rollback.** Parse and validate all import data before calling `clearCredentials()`. No snapshot/rollback needed. | Realistic failure is bad input data, not DataStore write failures. Simpler, less secret material in memory. |
| 10/11 | Chat history duplication | **VM owns history.** Remove `messages.add(userMessage)` at ChatEngine line 64. For regenerate, remove both last assistant and last user message before calling `sendMessage()`. | Single source of truth in the ViewModel. |

## Planned Follow-up (not in this fix cycle)

- **Tools UI overhaul** based on Kai's patterns: expandable server cards, per-server headers for auth, connection status indicators, per-tool toggles inside server cards. Separate feature issue.

---

## Recommended Fix Priority

### Priority 1 -- Security (ship-blocking)
1. **#1 BODY-level logging** -- trivial fix, high impact
2. **#2 Token leakage to MCP servers** -- credential exfiltration vector
3. **#3 Unencrypted LLM keys** -- use existing Tink infrastructure

### Priority 2 -- Write Safety (assistant trust boundary)
4. **#5 MCP confirmation bypass** -- destructive actions without consent
5. **#6 MCP cache on writes** -- silent no-ops on repeated writes
6. **#7 Missing _partial_update filter** -- read-only contract violation
7. **#16 Silent empty args execution** -- unpredictable tool behavior

### Priority 3 -- Data Integrity
8. **#4 Re-auth wipes MCP config** -- silent data loss on re-auth
9. **#9 Non-transactional backup replace** -- credential loss on import failure
10. **#13 MCP disable clears config** -- data loss on toggle

### Priority 4 -- Reliability and UX
11. **#8 Seen-before-notified approvals** -- missed approval alerts
12. **#10 Duplicate user messages** -- token waste, quality degradation
13. **#11 Regenerate compounds duplicates** -- progressive quality degradation
14. **#12 Workflow pagination** -- silent truncation
15. **#14 Dashboard race condition** -- stale/mixed data
16. **#15 Credential status conflation** -- wrong UX for offline users
