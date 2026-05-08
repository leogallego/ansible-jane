package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.model.McpServerConfig

object ToolRouter {

    private val WRITE_ACTIONS = setOf(
        "_create", "_update", "_delete",
        "_launch", "_relaunch", "_cancel"
    )

    private enum class Category(
        val keywords: Set<String>,
        val resourcePrefixes: Set<String>
    ) {
        INVENTORY(
            keywords = setOf("host", "hosts", "group", "groups", "inventory", "inventories", "infrastructure"),
            resourcePrefixes = setOf("hosts", "groups", "inventories", "constructed_inventories", "inventory_sources")
        ),
        JOBS(
            keywords = setOf("job", "jobs", "template", "templates", "launch", "run", "schedule", "schedules", "workflow", "playbook"),
            resourcePrefixes = setOf("jobs", "job_templates", "workflow_jobs", "workflow_job_templates", "workflow_job_nodes", "schedules", "ad_hoc_commands")
        ),
        MONITORING(
            keywords = setOf("health", "status", "monitor", "metrics", "log", "logs", "dashboard", "analytics", "instance", "instances"),
            resourcePrefixes = setOf("dashboard", "ping", "config", "instances", "instance_groups", "metrics")
        ),
        USERS(
            keywords = setOf("user", "users", "team", "teams", "organization", "organizations", "org", "role", "roles", "permission", "permissions", "member"),
            resourcePrefixes = setOf("users", "teams", "organizations", "roles", "tokens", "applications")
        ),
        SECURITY(
            keywords = setOf("credential", "credentials", "secret", "secrets", "audit", "security", "compliance", "policy", "certificate"),
            resourcePrefixes = setOf("credentials", "credential_types", "credential_input_sources")
        ),
        CONFIGURATION(
            keywords = setOf("setting", "settings", "configure", "configuration", "notification", "notifications", "label", "labels", "project", "projects"),
            resourcePrefixes = setOf("settings", "notification_templates", "notifications", "labels", "execution_environments", "projects")
        )
    }

    fun filterTools(
        query: String,
        tools: List<Tool>,
        serverConfigs: List<McpServerConfig>
    ): List<Tool> {
        val readOnlyLabels = serverConfigs
            .filter { it.readOnly }
            .map { it.label }
            .toSet()

        val accessFiltered = if (readOnlyLabels.isEmpty()) {
            tools
        } else {
            tools.filter { tool ->
                val serverLabel = tool.spec.description
                    .substringAfter("[", "")
                    .substringBefore("]", "")
                if (serverLabel in readOnlyLabels) {
                    WRITE_ACTIONS.none { action -> tool.spec.name.endsWith(action) }
                } else {
                    true
                }
            }
        }

        val queryWords = query.lowercase().split(Regex("\\W+")).toSet()

        val matchedCategories = Category.entries.filter { category ->
            category.keywords.any { it in queryWords }
        }

        if (matchedCategories.isEmpty()) return emptyList()

        val relevantPrefixes = matchedCategories
            .flatMap { it.resourcePrefixes }
            .toSet()

        return accessFiltered.filter { tool ->
            val resource = tool.spec.name
                .substringAfter(".")
                .substringBeforeLast("_")
            resource in relevantPrefixes
        }
    }
}
