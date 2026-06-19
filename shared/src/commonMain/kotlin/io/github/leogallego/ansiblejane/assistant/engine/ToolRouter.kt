package io.github.leogallego.ansiblejane.assistant.engine

import io.github.leogallego.ansiblejane.TestOnly
import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource
import io.github.leogallego.ansiblejane.model.McpServerConfig
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class ToolRouter(
    initialLocalTools: List<LocalTool> = emptyList(),
    private val repository: IAssistantRepository? = null
) : SynchronizedObject() {

    data class ToolKey(val name: String, val source: ToolSource, val serverLabel: String? = null) {
        fun toPersistedKey(): String = when (source) {
            ToolSource.LOCAL -> "LOCAL:$name"
            ToolSource.MCP -> "MCP:${serverLabel ?: ""}:$name"
        }

        companion object {
            fun fromPersistedKey(key: String): ToolKey? {
                val firstColon = key.indexOf(':')
                if (firstColon <= 0) return null
                val sourceStr = key.substring(0, firstColon)
                val source = try { ToolSource.valueOf(sourceStr) } catch (_: Exception) { return null }
                val rest = key.substring(firstColon + 1)
                return when (source) {
                    ToolSource.LOCAL -> ToolKey(rest, source)
                    ToolSource.MCP -> {
                        val secondColon = rest.indexOf(':')
                        if (secondColon >= 0) {
                            val serverLabel = rest.substring(0, secondColon).takeIf { it.isNotEmpty() }
                            val name = rest.substring(secondColon + 1)
                            ToolKey(name, source, serverLabel)
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    private val localTools = mutableListOf<LocalTool>()
    private val mcpTools = mutableListOf<Tool>()
    private val autoDisabled = mutableSetOf<ToolKey>()
    private val userDisabled = mutableSetOf<ToolKey>()
    private val userEnabled = mutableSetOf<ToolKey>()

    private val initialized = atomic(false)

    init {
        synchronized(this) {
            if (initialLocalTools.isNotEmpty()) {
                localTools.addAll(initialLocalTools)
                autoDisableOverlappingMcpTools()
            }
        }
    }

    suspend fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        val repo = repository ?: return
        val disabled = repo.getDisabledTools()
        val overrides = repo.getEnabledOverrides()
        applyPersistedState(disabled, overrides)
    }

    private enum class Category(
        val keywords: Set<String>,
        val resourcePrefixes: Set<String>,
        val localToolNames: Set<String>
    ) {
        INVENTORY(
            keywords = setOf(
                "host", "hosts", "group", "groups", "inventory", "inventories",
                "infrastructure", "facts", "gather", "info", "server", "servers",
                "machine", "machines", "asset", "assets", "source", "sources",
                "label", "labels", "tag", "tags", "summary", "summaries"
            ),
            resourcePrefixes = setOf("hosts", "groups", "inventories", "constructed_inventories", "inventory_sources", "labels"),
            localToolNames = setOf(
                "list_inventories", "list_hosts", "get_host_facts", "get_host_job_summaries",
                "list_groups", "list_inventory_sources", "list_labels"
            )
        ),
        JOBS(
            keywords = setOf(
                "job", "jobs", "template", "templates", "launch", "run",
                "schedule", "schedules", "workflow", "playbook", "jt", "wfjt",
                "output", "stdout", "running", "failed", "started", "task",
                "tasks", "command", "error", "errors", "failure", "status",
                "playbooks", "workflows", "execution", "executions",
                "survey", "node", "nodes", "prompt", "variable", "variables",
                "approval", "approvals", "approve", "deny", "pending"
            ),
            resourcePrefixes = setOf("jobs", "job_templates", "workflow_jobs", "workflow_job_templates", "workflow_job_nodes", "workflow_job_template_nodes", "schedules", "ad_hoc_commands", "workflow_approvals"),
            localToolNames = setOf(
                "list_job_templates", "launch_job", "get_job", "get_job_stdout", "list_jobs",
                "list_workflow_templates", "launch_workflow", "get_workflow_job",
                "list_schedules", "toggle_schedule",
                "list_workflow_nodes", "get_survey_spec",
                "list_pending_approvals", "approve_workflow", "deny_workflow"
            )
        ),
        MONITORING(
            keywords = setOf(
                "health", "status", "monitor", "metrics", "log", "logs",
                "dashboard", "analytics", "instance", "instances", "mesh",
                "topology", "ping", "cluster", "capacity", "node",
                "monitoring", "healthy", "overview", "nodes", "workers",
                "alive", "up", "down", "diagnostics", "group"
            ),
            resourcePrefixes = setOf("dashboard", "ping", "config", "instances", "instance_groups", "metrics", "mesh_visualizer"),
            localToolNames = setOf("list_instances", "get_instance", "list_instance_groups", "ping", "get_mesh_topology")
        ),
        USERS(
            keywords = setOf(
                "user", "users", "team", "teams", "organization", "organizations",
                "org", "role", "roles", "permission", "permissions", "member",
                "members", "people", "admin", "admins", "token", "tokens",
                "application", "applications", "app", "apps", "access", "rbac",
                "definition", "definitions", "oauth"
            ),
            resourcePrefixes = setOf("users", "teams", "organizations", "roles", "role_definitions", "tokens", "applications"),
            localToolNames = setOf(
                "list_organizations", "list_users", "list_teams",
                "list_roles", "list_role_definitions",
                "list_applications", "list_tokens"
            )
        ),
        SECURITY(
            keywords = setOf(
                "credential", "credentials", "secret", "secrets", "security",
                "compliance", "policy", "certificate", "creds", "vault",
                "password", "passwords", "key", "keys", "cert", "certs",
                "type", "types"
            ),
            resourcePrefixes = setOf("credentials", "credential_types", "credential_input_sources"),
            localToolNames = setOf("list_credentials", "get_credential", "list_credential_types")
        ),
        CONFIGURATION(
            keywords = setOf(
                "setting", "settings", "configure", "configuration", "notification",
                "notifications", "label", "labels", "project", "projects",
                "execution", "environment", "environments", "config", "ee",
                "ees", "scm", "repo", "repos", "repository", "alert", "alerts",
                "tag", "tags", "license", "subscription", "version"
            ),
            resourcePrefixes = setOf("settings", "notification_templates", "notifications", "labels", "execution_environments", "projects", "config"),
            localToolNames = setOf(
                "list_projects", "get_project", "list_execution_environments",
                "list_notification_templates", "get_settings", "get_config"
            )
        ),
        EDA(
            keywords = setOf(
                "eda", "rulebook", "activation", "event", "audit", "de", "des",
                "rule", "rules", "trigger", "triggers", "webhook", "webhooks",
                "stream", "streams", "decision", "driven", "rulebooks",
                "activations", "events", "environment"
            ),
            resourcePrefixes = setOf("audit_rules", "activations", "decision_environments", "rulebooks", "event_streams", "eda_credentials", "eda_credential_types", "eda_projects", "eda_users"),
            localToolNames = setOf(
                "list_eda_audit_rules", "list_eda_activations", "get_eda_activation",
                "list_eda_rulebooks", "list_eda_decision_environments",
                "list_eda_projects", "list_eda_credentials", "list_eda_credential_types",
                "list_eda_event_streams", "list_eda_users"
            )
        ),
        PLATFORM(
            keywords = setOf(
                "platform", "gateway", "authenticator", "authenticators",
                "service", "services", "sso", "saml", "ldap",
                "identity", "authentication", "provider", "providers"
            ),
            resourcePrefixes = setOf("organizations", "users", "teams", "role_definitions", "authenticators", "services", "service_clusters"),
            localToolNames = setOf(
                "list_platform_organizations", "list_platform_users", "list_platform_teams",
                "list_platform_role_definitions", "list_authenticators",
                "list_platform_services", "list_service_clusters"
            )
        );

        val stemmedKeywords: Set<String> by lazy {
            keywords.map { stem(it) }.toSet()
        }
    }

    companion object {
        private const val TAG = "ToolRouter"

        val OVERLAP_MAPPING = mapOf(
            "list_job_templates" to setOf("controller.job_templates_list"),
            "launch_job" to setOf("controller.job_templates_launch_create"),
            "get_job" to setOf("controller.jobs_read"),
            "get_job_stdout" to setOf("controller.jobs_stdout_read"),
            "list_jobs" to setOf("controller.jobs_list"),
            "list_workflow_templates" to setOf("controller.workflow_job_templates_list"),
            "launch_workflow" to setOf("controller.workflow_job_templates_launch_create"),
            "get_workflow_job" to setOf("controller.workflow_jobs_read"),
            "list_inventories" to setOf("controller.inventories_list"),
            "list_hosts" to setOf("controller.hosts_list"),
            "get_host_facts" to setOf("controller.hosts_ansible_facts_read"),
            "list_schedules" to setOf("controller.schedules_list"),
            "toggle_schedule" to setOf("controller.schedules_partial_update", "controller.schedules_update"),
            "list_eda_audit_rules" to setOf("eda.audit_rules_list"),
            "list_instances" to setOf("controller.instances_list"),
            "get_instance" to setOf("controller.instances_read"),
            "list_instance_groups" to setOf("controller.instance_groups_list"),
            "ping" to setOf("controller.ping_read"),
            "get_mesh_topology" to setOf("controller.mesh_visualizer_read"),
            "list_credentials" to setOf("controller.credentials_list"),
            "get_credential" to setOf("controller.credentials_read"),
            "list_projects" to setOf("controller.projects_list"),
            "get_project" to setOf("controller.projects_read"),
            "list_execution_environments" to setOf("controller.execution_environments_list"),
            "list_eda_activations" to setOf("eda.activations_list"),
            "get_eda_activation" to setOf("eda.activations_read"),
            "list_organizations" to setOf("controller.organizations_list"),
            "list_users" to setOf("controller.users_list"),
            "list_teams" to setOf("controller.teams_list"),
            "list_roles" to setOf("controller.roles_list"),
            "list_role_definitions" to setOf("controller.role_definitions_list"),
            "list_groups" to setOf("controller.groups_list"),
            "list_inventory_sources" to setOf("controller.inventory_sources_list"),
            "list_labels" to setOf("controller.labels_list"),
            "get_host_job_summaries" to setOf("controller.hosts_job_host_summaries_list"),
            "list_credential_types" to setOf("controller.credential_types_list"),
            "list_notification_templates" to setOf("controller.notification_templates_list"),
            "list_applications" to setOf("controller.applications_list"),
            "list_tokens" to setOf("controller.tokens_list"),
            "get_settings" to setOf("controller.settings_list"),
            "get_config" to setOf("controller.config_read"),
            "list_workflow_nodes" to setOf("controller.workflow_job_template_nodes_list"),
            "get_survey_spec" to setOf("controller.job_templates_survey_spec_read"),
            "list_pending_approvals" to setOf("controller.workflow_approvals_list"),
            "approve_workflow" to setOf("controller.workflow_approvals_approve_create"),
            "deny_workflow" to setOf("controller.workflow_approvals_deny_create"),
            "list_eda_rulebooks" to setOf("eda.rulebooks_list"),
            "list_eda_decision_environments" to setOf("eda.decision_environments_list"),
            "list_eda_projects" to setOf("eda.projects_list"),
            "list_eda_credentials" to setOf("eda.credentials_list"),
            "list_eda_credential_types" to setOf("eda.credential_types_list"),
            "list_eda_event_streams" to setOf("eda.event_streams_list"),
            "list_eda_users" to setOf("eda.users_list"),
            // Platform / Gateway
            "list_platform_organizations" to setOf("gateway.organizations_list"),
            "list_platform_users" to setOf("gateway.users_list"),
            "list_platform_teams" to setOf("gateway.teams_list"),
            "list_platform_role_definitions" to setOf("gateway.role_definitions_list"),
            "list_authenticators" to setOf("gateway.authenticators_list"),
            "list_platform_services" to setOf("gateway.services_list"),
            "list_service_clusters" to setOf("gateway.service_clusters_list"),
        )

        private val MCP_TOOLS_WITH_LOCAL_OVERLAP: Set<String> =
            OVERLAP_MAPPING.values.flatten().toSet()

        private val WRITE_ACTIONS = Tool.WRITE_SUFFIXES

        private val STOP_WORDS = setOf(
            "list", "get", "show", "what", "are", "the", "is", "a", "an",
            "my", "all", "me", "how", "many", "which", "do", "i", "have",
            "can", "tell", "about", "find", "check", "give",
            "of", "for", "in", "on", "to", "and", "or", "with", "from",
            "any", "been"
        )

        private val TOOLSET_CATEGORY_MAP = mapOf(
            "job_management" to setOf(Category.JOBS),
            "inventory_management" to setOf(Category.INVENTORY),
            "system_monitoring" to setOf(Category.MONITORING),
            "user_management" to setOf(Category.USERS),
            "security_compliance" to setOf(Category.SECURITY),
            "platform_configuration" to setOf(Category.CONFIGURATION),
            "event_management" to setOf(Category.EDA),
            "integration" to setOf(Category.CONFIGURATION, Category.SECURITY, Category.USERS),
            "developer_integration" to setOf(Category.JOBS, Category.MONITORING),
        )

        fun getCategoryForTool(toolName: String): String? {
            return Category.entries.firstOrNull { toolName in it.localToolNames }?.name
        }

        fun stem(word: String): String {
            val result = word
                .removeSuffix("ies").let { if (it != word) "${it}y" else it }
                .removeSuffix("es")
                .removeSuffix("s")
                .removeSuffix("e")
            return if (result.length < 2) word else result
        }
    }

    fun registerLocalTools(tools: List<LocalTool>) = synchronized(this) {
        localTools.clear()
        localTools.addAll(tools)
        autoDisableOverlappingMcpTools()
    }

    fun registerMcpTools(tools: List<Tool>) = synchronized(this) {
        mcpTools.clear()
        mcpTools.addAll(tools)
        autoDisableOverlappingMcpTools()
    }

    @TestOnly
    fun setToolEnabled(toolName: String, source: ToolSource, serverLabel: String? = null, enabled: Boolean) = synchronized(this) {
        val key = ToolKey(toolName, source, serverLabel)
        if (enabled) {
            userDisabled.remove(key)
            userEnabled.add(key)
        } else {
            userDisabled.add(key)
            userEnabled.remove(key)
        }
    }

    fun isToolEnabled(toolName: String, source: ToolSource, serverLabel: String? = null): Boolean = synchronized(this) {
        val key = ToolKey(toolName, source, serverLabel)
        val isAuto = isAutoDisabledByName(toolName, source)
        key !in userDisabled && (!isAuto || key in userEnabled)
    }

    fun isAutoDisabled(toolName: String, source: ToolSource, serverLabel: String? = null): Boolean = synchronized(this) {
        isAutoDisabledByName(toolName, source)
    }

    private fun isAutoDisabledByName(toolName: String, source: ToolSource): Boolean {
        return autoDisabled.any { it.name == toolName && it.source == source }
    }

    suspend fun toggleToolEnabled(toolName: String, source: ToolSource, serverLabel: String? = null, enabled: Boolean) {
        val snapshot = synchronized(this) {
            val key = ToolKey(toolName, source, serverLabel)
            val isAuto = isAutoDisabledByName(toolName, source)
            if (isAuto) {
                userDisabled.remove(key)
                if (enabled) userEnabled.add(key) else userEnabled.remove(key)
            } else {
                if (enabled) userDisabled.remove(key) else userDisabled.add(key)
                userEnabled.remove(key)
            }
            Pair(
                userDisabled.map { it.toPersistedKey() }.toSet(),
                userEnabled.map { it.toPersistedKey() }.toSet()
            )
        }
        repository?.saveToolState(snapshot.first, snapshot.second)
    }

    fun getPersistedDisabled(): Set<String> = synchronized(this) {
        userDisabled.map { it.toPersistedKey() }.toSet()
    }

    fun getPersistedOverrides(): Set<String> = synchronized(this) {
        userEnabled.map { it.toPersistedKey() }.toSet()
    }

    fun applyPersistedState(disabled: Set<String>, enabledOverrides: Set<String>) = synchronized(this) {
        for (entry in disabled) {
            val key = ToolKey.fromPersistedKey(entry) ?: continue
            userDisabled.add(key)
        }
        for (entry in enabledOverrides) {
            val key = ToolKey.fromPersistedKey(entry) ?: continue
            userEnabled.add(key)
        }
    }

    data class QueryResult(
        val tools: List<Tool>,
        val categoryMatched: Boolean
    )

    fun getToolsForQuery(
        query: String,
        serverConfigs: List<McpServerConfig> = emptyList()
    ): QueryResult = synchronized(this) {
        val queryWords = query.lowercase().split(Regex("\\W+")).toSet()
        val stemmedQuery = (queryWords - STOP_WORDS).map { stem(it) }.toSet()
        Log.d(TAG, "QUERY: words=$queryWords, stemmed=$stemmedQuery")

        val matchedCategories = Category.entries.filter { category ->
            category.stemmedKeywords.any { it in stemmedQuery }
        }

        if (matchedCategories.isEmpty()) {
            Log.d(TAG, "QUERY: no categories matched")
            return@synchronized QueryResult(emptyList(), categoryMatched = false)
        }
        Log.d(TAG, "QUERY: matched categories=${matchedCategories.map { it.name }}")

        val matchedLocalNames = matchedCategories.flatMap { it.localToolNames }.toSet()

        val readOnlyLabels = serverConfigs
            .filter { it.readOnly }
            .map { it.label }
            .toSet()

        val filteredLocal = localTools.filter { tool ->
            tool.spec.name in matchedLocalNames &&
                isToolEnabled(tool.spec.name, ToolSource.LOCAL)
        }

        val routedMcp = mutableListOf<Tool>()
        val unroutedMcp = mutableListOf<Tool>()

        for (tool in mcpTools) {
            if (!isToolEnabled(tool.spec.name, ToolSource.MCP, tool.serverLabel)) continue

            val passesReadOnly = if (readOnlyLabels.isNotEmpty()) {
                if (tool.serverLabel in readOnlyLabels) {
                    WRITE_ACTIONS.none { action -> tool.spec.name.endsWith(action) }
                } else true
            } else true
            if (!passesReadOnly) continue

            val toolToolset = tool.toolset
            val toolsetCategories = toolToolset?.let { TOOLSET_CATEGORY_MAP[it] }
            when {
                toolsetCategories != null && matchedCategories.any { it in toolsetCategories } ->
                    routedMcp.add(tool)
                toolToolset != null && toolsetCategories == null -> {
                    Log.d(TAG, "FILTER: unknown toolset '${toolToolset}' for ${tool.spec.name}, treating as unrouted")
                    unroutedMcp.add(tool)
                }
                else ->
                    unroutedMcp.add(tool)
            }
        }

        Log.d(TAG, "FILTER: ${filteredLocal.size} local, ${routedMcp.size} routed mcp, ${unroutedMcp.size} unrouted mcp")
        val cherryPickedLocal = cherryPick(filteredLocal, stemmedQuery)
        val cherryPickedRouted = cherryPick(routedMcp, stemmedQuery)
        val cherryPickedUnrouted = cherryPick(unroutedMcp, stemmedQuery, requireOverlap = true)
        val allCherryPicked = cherryPickedLocal + cherryPickedRouted + cherryPickedUnrouted
        Log.d(TAG, "CHERRY: ${cherryPickedLocal.size} local, ${cherryPickedRouted.size} routed, " +
            "${cherryPickedUnrouted.size} unrouted [${allCherryPicked.map { it.spec.name }}]")

        QueryResult(allCherryPicked, categoryMatched = true)
    }

    private fun cherryPick(
        tools: List<Tool>,
        stemmedQuery: Set<String>,
        requireOverlap: Boolean = false
    ): List<Tool> {
        val scored = tools.map { tool ->
            val nameParts = tool.spec.name
                .split(".", "_")
                .map { stem(it) }
                .toSet()
            val overlap = (nameParts intersect stemmedQuery).size
            var score = overlap * 10
            if (tool.spec.name.contains("list") || tool.spec.name.contains("ping")) score += 3
            if (tool.spec.name.contains("get") || tool.spec.name.contains("read")) score += 1
            if (overlap > 0 && tool.isDestructive) score -= 5
            Triple(tool, score, overlap)
        }
        Log.d(TAG, "SCORES: ${scored.map { "${it.first.spec.name}=${it.second}" }}")
        return scored
            .filter { it.second > 0 && (!requireOverlap || it.third > 0) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun getAllRegisteredTools(): List<Pair<Tool, ToolSource>> = synchronized(this) {
        val result = mutableListOf<Pair<Tool, ToolSource>>()
        localTools.forEach { result.add(it to ToolSource.LOCAL) }
        mcpTools.forEach { result.add(it to ToolSource.MCP) }
        result
    }

    private fun autoDisableOverlappingMcpTools() {
        autoDisabled.clear()
        val activeLocalNames = localTools.map { it.spec.name }.toSet()
        var disabledCount = 0
        for (localName in activeLocalNames) {
            val overlappingMcpNames = OVERLAP_MAPPING[localName] ?: continue
            for (mcpName in overlappingMcpNames) {
                autoDisabled.add(ToolKey(mcpName, ToolSource.MCP))
                disabledCount++
            }
        }
        if (disabledCount > 0) {
            Log.d(TAG, "OVERLAP: disabled $disabledCount MCP tools overlapping with ${activeLocalNames.size} local tools")
        }
    }

}
