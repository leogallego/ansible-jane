package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolSource
import com.example.aapremote.model.McpServerConfig

class ToolRouter {

    private val localTools = mutableListOf<LocalTool>()
    private val mcpTools = mutableListOf<Tool>()
    private val disabledTools = mutableSetOf<Pair<String, ToolSource>>()

    private enum class Category(
        val keywords: Set<String>,
        val resourcePrefixes: Set<String>,
        val localToolNames: Set<String>
    ) {
        INVENTORY(
            keywords = setOf(
                "host", "hosts", "group", "groups", "inventory", "inventories",
                "infrastructure", "facts", "gather", "info", "server", "servers",
                "machine", "machines", "asset", "assets"
            ),
            resourcePrefixes = setOf("hosts", "groups", "inventories", "constructed_inventories", "inventory_sources"),
            localToolNames = setOf("list_inventories", "list_hosts", "get_host_facts")
        ),
        JOBS(
            keywords = setOf(
                "job", "jobs", "template", "templates", "launch", "run",
                "schedule", "schedules", "workflow", "playbook", "jt", "wfjt",
                "output", "stdout", "running", "failed", "started", "task",
                "tasks", "command", "error", "errors", "failure", "status",
                "playbooks", "workflows", "execution", "executions"
            ),
            resourcePrefixes = setOf("jobs", "job_templates", "workflow_jobs", "workflow_job_templates", "workflow_job_nodes", "schedules", "ad_hoc_commands"),
            localToolNames = setOf(
                "list_job_templates", "launch_job", "get_job", "get_job_stdout", "list_jobs",
                "list_workflow_templates", "launch_workflow", "get_workflow_job",
                "list_schedules", "toggle_schedule"
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
                "application", "applications", "app", "apps", "access", "rbac"
            ),
            resourcePrefixes = setOf("users", "teams", "organizations", "roles", "tokens", "applications"),
            localToolNames = emptySet()
        ),
        SECURITY(
            keywords = setOf(
                "credential", "credentials", "secret", "secrets", "security",
                "compliance", "policy", "certificate", "creds", "vault",
                "password", "passwords", "key", "keys", "cert", "certs"
            ),
            resourcePrefixes = setOf("credentials", "credential_types", "credential_input_sources"),
            localToolNames = setOf("list_credentials", "get_credential")
        ),
        CONFIGURATION(
            keywords = setOf(
                "setting", "settings", "configure", "configuration", "notification",
                "notifications", "label", "labels", "project", "projects",
                "execution", "environment", "environments", "config", "ee",
                "ees", "scm", "repo", "repos", "repository", "alert", "alerts",
                "tag", "tags"
            ),
            resourcePrefixes = setOf("settings", "notification_templates", "notifications", "labels", "execution_environments", "projects"),
            localToolNames = setOf("list_projects", "get_project", "list_execution_environments")
        ),
        EDA(
            keywords = setOf(
                "eda", "rulebook", "activation", "event", "audit", "de",
                "rule", "rules", "trigger", "triggers", "webhook", "webhooks",
                "stream", "streams", "decision", "driven", "rulebooks",
                "activations", "events", "environment"
            ),
            resourcePrefixes = setOf("audit_rules", "activations", "decision_environments", "rulebooks", "event_streams"),
            localToolNames = setOf("list_eda_audit_rules", "list_eda_activations", "get_eda_activation")
        );

        val stemmedKeywords: Set<String> by lazy {
            keywords.map { stem(it) }.toSet()
        }
    }

    companion object {
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
        )

        private val MCP_TOOLS_WITH_LOCAL_OVERLAP: Set<String> =
            OVERLAP_MAPPING.values.flatten().toSet()

        private val WRITE_ACTIONS = setOf(
            "_create", "_update", "_delete",
            "_launch", "_relaunch", "_cancel"
        )

        private val STOP_WORDS = setOf(
            "list", "get", "show", "what", "are", "the", "is", "a", "an",
            "my", "all", "me", "how", "many", "which", "do", "i", "have",
            "can", "tell", "about", "find", "check", "give",
            "of", "for", "in", "on", "to", "and", "or", "with", "from",
            "any", "been"
        )

        fun stem(word: String): String {
            val result = word
                .removeSuffix("ies").let { if (it != word) "${it}y" else it }
                .removeSuffix("es")
                .removeSuffix("s")
                .removeSuffix("e")
            return if (result.length < 3) word else result
        }
    }

    fun registerLocalTools(tools: List<LocalTool>) {
        localTools.clear()
        localTools.addAll(tools)
        autoDisableOverlappingMcpTools()
    }

    fun registerMcpTools(tools: List<Tool>) {
        mcpTools.clear()
        mcpTools.addAll(tools)
        autoDisableOverlappingMcpTools()
    }

    fun setToolEnabled(toolName: String, source: ToolSource, enabled: Boolean) {
        val key = toolName to source
        if (enabled) disabledTools.remove(key) else disabledTools.add(key)
    }

    fun isToolEnabled(toolName: String, source: ToolSource): Boolean {
        return (toolName to source) !in disabledTools
    }

    data class QueryResult(
        val tools: List<Tool>,
        val categoryMatched: Boolean
    )

    fun getToolsForQuery(
        query: String,
        serverConfigs: List<McpServerConfig> = emptyList()
    ): QueryResult {
        val queryWords = query.lowercase().split(Regex("\\W+")).toSet()
        val stemmedQuery = (queryWords - STOP_WORDS).map { stem(it) }.toSet()

        val matchedCategories = Category.entries.filter { category ->
            category.stemmedKeywords.any { it in stemmedQuery }
        }

        if (matchedCategories.isEmpty()) return QueryResult(emptyList(), categoryMatched = false)

        val matchedLocalNames = matchedCategories.flatMap { it.localToolNames }.toSet()
        val matchedPrefixes = matchedCategories.flatMap { it.resourcePrefixes }.toSet()

        val readOnlyLabels = serverConfigs
            .filter { it.readOnly }
            .map { it.label }
            .toSet()

        val filteredLocal = localTools.filter { tool ->
            tool.spec.name in matchedLocalNames &&
                isToolEnabled(tool.spec.name, ToolSource.LOCAL)
        }

        val filteredMcp = mcpTools.filter { tool ->
            val resource = tool.spec.name
                .substringAfter(".")
                .substringBeforeLast("_")
            val matchesCategory = resource in matchedPrefixes
            val isEnabled = isToolEnabled(tool.spec.name, ToolSource.MCP)

            val passesReadOnly = if (readOnlyLabels.isNotEmpty()) {
                val serverLabel = tool.spec.description
                    .substringAfter("[", "")
                    .substringBefore("]", "")
                if (serverLabel in readOnlyLabels) {
                    WRITE_ACTIONS.none { action -> tool.spec.name.endsWith(action) }
                } else {
                    true
                }
            } else {
                true
            }

            matchesCategory && isEnabled && passesReadOnly
        }

        val cherryPickedLocal = cherryPick(filteredLocal, stemmedQuery)
        val cherryPickedMcp = cherryPick(filteredMcp, stemmedQuery)

        return QueryResult(cherryPickedLocal + cherryPickedMcp, categoryMatched = true)
    }

    private fun cherryPick(tools: List<Tool>, stemmedQuery: Set<String>): List<Tool> {
        return tools.map { tool ->
            val nameParts = tool.spec.name
                .split(".", "_")
                .map { stem(it) }
                .toSet()
            val overlap = (nameParts intersect stemmedQuery).size
            var score = overlap * 10
            if (tool.spec.name.contains("list") || tool.spec.name.contains("ping")) score += 3
            if (tool.spec.name.contains("get") || tool.spec.name.contains("read")) score += 1
            if (overlap > 0 && tool is LocalTool && tool.destructive) score -= 5
            tool to score
        }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun getAllRegisteredTools(): List<Pair<Tool, ToolSource>> {
        val result = mutableListOf<Pair<Tool, ToolSource>>()
        localTools.forEach { result.add(it to ToolSource.LOCAL) }
        mcpTools.forEach { result.add(it to ToolSource.MCP) }
        return result
    }

    private fun autoDisableOverlappingMcpTools() {
        val activeLocalNames = localTools.map { it.spec.name }.toSet()
        for (localName in activeLocalNames) {
            val overlappingMcpNames = OVERLAP_MAPPING[localName] ?: continue
            for (mcpName in overlappingMcpNames) {
                disabledTools.add(mcpName to ToolSource.MCP)
            }
        }
    }
}
