package io.github.leogallego.ansiblejane.assistant.engine

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.model.McpServerConfig
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToolRouterTest {

    private lateinit var router: ToolRouter

    private fun mcpTool(name: String, serverLabel: String = "aap") = object : Tool {
        override val spec = ToolSpec(name, "[$serverLabel] description of $name", JsonObject(emptyMap()))
        override suspend fun execute(args: JsonObject) = ToolResult(success = true)
    }

    private fun localTool(name: String, destructive: Boolean = false) = object : LocalTool(
        spec = ToolSpec(name, "Local: $name", JsonObject(emptyMap())),
        destructive = destructive
    ) {
        override suspend fun execute(args: JsonObject) = ToolResult(success = true)
    }

    @Before
    fun setup() {
        router = ToolRouter()
    }

    // --- Category matching tests ---

    @Test
    fun `SHOULD select inventory tools WHEN query mentions hosts`() {
        val inventoryLocal = localTool("list_hosts")
        val inventoryLocal2 = localTool("list_inventories")
        val jobLocal = localTool("list_jobs")

        router.registerLocalTools(listOf(inventoryLocal, inventoryLocal2, jobLocal))
        val result = router.getToolsForQuery("list my hosts").tools
        val names = result.map { it.spec.name }

        assertTrue("list_hosts" in names)
        assertTrue("list_inventories" in names)
        assertFalse("list_jobs" in names)
    }

    @Test
    fun `SHOULD select job tools WHEN query mentions launch and template`() {
        val tools = listOf(
            localTool("list_job_templates"),
            localTool("launch_job", destructive = true),
            localTool("get_job"),
            localTool("list_hosts")
        )

        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("launch a job template").tools
        val names = result.map { it.spec.name }

        assertTrue("list_job_templates" in names)
        assertTrue("launch_job" in names)
        assertTrue("get_job" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD return no tools WHEN no category matches`() {
        val tools = listOf(
            localTool("list_hosts"),
            localTool("list_jobs"),
            localTool("list_job_templates")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("hello how are you").tools
        assertTrue(result.isEmpty())
    }

    @Test
    fun `SHOULD return no tools WHEN query is a greeting`() {
        val tools = listOf(
            localTool("list_hosts"),
            localTool("list_jobs"),
            localTool("list_job_templates")
        )
        router.registerLocalTools(tools)

        assertTrue(router.getToolsForQuery("hi").tools.isEmpty())
        assertTrue(router.getToolsForQuery("hello there").tools.isEmpty())
        assertTrue(router.getToolsForQuery("what can you do?").tools.isEmpty())
        assertTrue(router.getToolsForQuery("thanks").tools.isEmpty())
    }

    @Test
    fun `SHOULD include tools from multiple matched categories WHEN query spans categories`() {
        val tools = listOf(
            localTool("list_hosts"),
            localTool("list_jobs"),
            localTool("get_job"),
            localTool("list_eda_audit_rules")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show hosts running failed jobs").tools
        val names = result.map { it.spec.name }

        assertTrue("list_hosts" in names)
        assertTrue("list_jobs" in names)
        assertTrue("get_job" in names)
    }

    @Test
    fun `SHOULD select EDA tools WHEN query mentions eda or rulebook`() {
        val tools = listOf(
            localTool("list_eda_audit_rules"),
            localTool("list_hosts"),
            localTool("list_jobs")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show eda audit rules").tools
        val names = result.map { it.spec.name }

        assertTrue("list_eda_audit_rules" in names)
        assertFalse("list_hosts" in names)
        assertFalse("list_jobs" in names)
    }

    // --- Local tool priority tests ---

    @Test
    fun `SHOULD auto-disable overlapping MCP tools WHEN local tools are registered`() {
        val localTools = listOf(
            localTool("list_job_templates"),
            localTool("list_hosts")
        )
        router.registerLocalTools(localTools)

        assertFalse(router.isToolEnabled("controller.job_templates_list", ToolSource.MCP))
        assertFalse(router.isToolEnabled("controller.hosts_list", ToolSource.MCP))
        assertTrue(router.isToolEnabled("controller.users_list", ToolSource.MCP))
    }

    @Test
    fun `SHOULD allow re-enabling MCP tools via setToolEnabled`() {
        router.registerLocalTools(listOf(localTool("list_job_templates")))
        assertFalse(router.isToolEnabled("controller.job_templates_list", ToolSource.MCP))

        router.setToolEnabled("controller.job_templates_list", ToolSource.MCP, true)
        assertTrue(router.isToolEnabled("controller.job_templates_list", ToolSource.MCP))
    }

    @Test
    fun `SHOULD allow disabling local tools via setToolEnabled`() {
        router.registerLocalTools(listOf(localTool("list_hosts")))
        assertTrue(router.isToolEnabled("list_hosts", ToolSource.LOCAL))

        router.setToolEnabled("list_hosts", ToolSource.LOCAL, false)
        assertFalse(router.isToolEnabled("list_hosts", ToolSource.LOCAL))
    }

    @Test
    fun `SHOULD not include disabled local tools in query results`() {
        val tools = listOf(
            localTool("list_hosts"),
            localTool("list_inventories")
        )
        router.registerLocalTools(tools)
        router.setToolEnabled("list_hosts", ToolSource.LOCAL, false)

        val result = router.getToolsForQuery("show my hosts and inventories").tools
        val names = result.map { it.spec.name }

        assertFalse("list_hosts" in names)
        assertTrue("list_inventories" in names)
    }

    @Test
    fun `SHOULD return empty WHEN no tools registered at all`() {
        val result = router.getToolsForQuery("list hosts").tools
        assertTrue(result.isEmpty())
    }

    @Test
    fun `SHOULD list all registered tools via getAllRegisteredTools`() {
        router.registerLocalTools(listOf(localTool("list_hosts"), localTool("list_jobs")))
        val all = router.getAllRegisteredTools()

        assertEquals(2, all.size)
        assertTrue(all.all { it.second == ToolSource.LOCAL })
    }

    @Test
    fun `SHOULD preserve destructive flag on local tools`() {
        val destructive = localTool("launch_job", destructive = true)
        val readOnly = localTool("list_jobs", destructive = false)

        router.registerLocalTools(listOf(destructive, readOnly))
        val all = router.getAllRegisteredTools()

        val launchTool = all.first { it.first.spec.name == "launch_job" }.first as LocalTool
        val listTool = all.first { it.first.spec.name == "list_jobs" }.first as LocalTool

        assertTrue(launchTool.destructive)
        assertFalse(listTool.destructive)
    }

    @Test
    fun `SHOULD select workflow tools WHEN query mentions workflow`() {
        val tools = listOf(
            localTool("list_workflow_templates"),
            localTool("launch_workflow", destructive = true),
            localTool("get_workflow_job"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show my workflow templates").tools
        val names = result.map { it.spec.name }

        assertTrue("list_workflow_templates" in names)
        assertTrue("launch_workflow" in names)
        assertTrue("get_workflow_job" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD select schedule tools WHEN query mentions schedules`() {
        val tools = listOf(
            localTool("list_schedules"),
            localTool("toggle_schedule", destructive = true),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show my schedules").tools
        val names = result.map { it.spec.name }

        assertTrue("list_schedules" in names)
        assertTrue("toggle_schedule" in names)
        assertFalse("list_hosts" in names)
    }

    // --- MCP tool tests ---

    private val readOnlyConfig = McpServerConfig(url = "https://aap:8448/mcp", label = "aap", readOnly = true)
    private val readWriteConfig = McpServerConfig(url = "https://aap:8448/mcp", label = "aap", readOnly = false)

    @Test
    fun `SHOULD exclude write MCP tools WHEN readOnly is true`() {
        val tools = listOf(
            mcpTool("controller.hosts_list"),
            mcpTool("controller.hosts_create"),
            mcpTool("controller.hosts_update"),
            mcpTool("controller.hosts_delete"),
            mcpTool("controller.inventories_list")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("list my hosts", listOf(readOnlyConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.hosts_list" in names)
        assertTrue("controller.inventories_list" in names)
        assertFalse("controller.hosts_create" in names)
        assertFalse("controller.hosts_update" in names)
        assertFalse("controller.hosts_delete" in names)
    }

    @Test
    fun `SHOULD keep all MCP tools WHEN readOnly is false`() {
        val tools = listOf(
            mcpTool("controller.hosts_list"),
            mcpTool("controller.hosts_create"),
            mcpTool("controller.hosts_delete")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("list hosts", listOf(readWriteConfig)).tools
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `SHOULD exclude launch and cancel MCP actions WHEN readOnly is true`() {
        val tools = listOf(
            mcpTool("controller.jobs_read"),
            mcpTool("controller.job_templates_launch"),
            mcpTool("controller.jobs_relaunch"),
            mcpTool("controller.jobs_cancel")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("show me jobs", listOf(readOnlyConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.jobs_read" in names)
        assertFalse("controller.job_templates_launch" in names)
        assertFalse("controller.jobs_relaunch" in names)
        assertFalse("controller.jobs_cancel" in names)
    }

    @Test
    fun `SHOULD only filter matching server label WHEN multiple MCP servers configured`() {
        val tools = listOf(
            mcpTool("controller.hosts_create", "aap"),
            mcpTool("controller.hosts_list", "aap"),
            mcpTool("controller.hosts_create", "knowledge")
        )
        val configs = listOf(
            McpServerConfig(url = "https://aap:8448/mcp", label = "aap", readOnly = true),
            McpServerConfig(url = "https://kb:3000/mcp", label = "knowledge", readOnly = false)
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("list hosts", configs).tools
        val names = result.map { it.spec.name to it.spec.description }

        assertTrue(names.any { it.first == "controller.hosts_list" && it.second.contains("[aap]") })
        assertFalse(names.any { it.first == "controller.hosts_create" && it.second.contains("[aap]") })
        assertTrue(names.any { it.first == "controller.hosts_create" && it.second.contains("[knowledge]") })
    }

    @Test
    fun `SHOULD select MCP inventory tools WHEN query mentions hosts`() {
        val tools = listOf(
            mcpTool("controller.hosts_list"),
            mcpTool("controller.groups_list"),
            mcpTool("controller.jobs_read"),
            mcpTool("controller.users_list")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("list my hosts", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.hosts_list" in names)
        assertTrue("controller.groups_list" in names)
        assertFalse("controller.jobs_read" in names)
        assertFalse("controller.users_list" in names)
    }

    @Test
    fun `SHOULD select MCP job tools WHEN query mentions launch and template`() {
        val tools = listOf(
            mcpTool("controller.hosts_list"),
            mcpTool("controller.job_templates_list"),
            mcpTool("controller.job_templates_launch"),
            mcpTool("controller.jobs_read")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("launch a job template", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.job_templates_list" in names)
        assertTrue("controller.job_templates_launch" in names)
        assertTrue("controller.jobs_read" in names)
        assertFalse("controller.hosts_list" in names)
    }

    @Test
    fun `SHOULD select MCP monitoring tools WHEN query mentions health`() {
        val tools = listOf(
            mcpTool("controller.hosts_list"),
            mcpTool("controller.instances_list"),
            mcpTool("controller.ping_read"),
            mcpTool("controller.dashboard_read")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("check system health", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.instances_list" in names)
        assertTrue("controller.ping_read" in names)
        assertTrue("controller.dashboard_read" in names)
        assertFalse("controller.hosts_list" in names)
    }

    @Test
    fun `SHOULD select MCP user tools WHEN query mentions users or teams`() {
        val tools = listOf(
            mcpTool("controller.users_list"),
            mcpTool("controller.teams_list"),
            mcpTool("controller.organizations_list"),
            mcpTool("controller.hosts_list")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("list users in my team", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.users_list" in names)
        assertTrue("controller.teams_list" in names)
        assertTrue("controller.organizations_list" in names)
        assertFalse("controller.hosts_list" in names)
    }

    @Test
    fun `SHOULD select MCP security tools WHEN query mentions credentials`() {
        val tools = listOf(
            mcpTool("controller.credentials_list"),
            mcpTool("controller.credential_types_list"),
            mcpTool("controller.hosts_list")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("show my credentials", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.credentials_list" in names)
        assertTrue("controller.credential_types_list" in names)
        assertFalse("controller.hosts_list" in names)
    }

    @Test
    fun `SHOULD select MCP config tools WHEN query mentions settings or projects`() {
        val tools = listOf(
            mcpTool("controller.settings_read"),
            mcpTool("controller.projects_list"),
            mcpTool("controller.notification_templates_list"),
            mcpTool("controller.hosts_list")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("show project settings", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.settings_read" in names)
        assertTrue("controller.projects_list" in names)
        assertTrue("controller.notification_templates_list" in names)
        assertFalse("controller.hosts_list" in names)
    }

    @Test
    fun `SHOULD parse compound MCP resource names correctly`() {
        val tools = listOf(
            mcpTool("controller.workflow_job_templates_list"),
            mcpTool("controller.constructed_inventories_list"),
            mcpTool("controller.hosts_list")
        )
        router.registerMcpTools(tools)
        val result = router.getToolsForQuery("show my workflow templates", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.workflow_job_templates_list" in names)
        assertFalse("controller.hosts_list" in names)
    }

    @Test
    fun `SHOULD return no MCP tools WHEN query matches no category`() {
        val tools = listOf(
            mcpTool("controller.hosts_list"),
            mcpTool("controller.jobs_read"),
            mcpTool("controller.users_list")
        )
        router.registerMcpTools(tools)

        assertTrue(router.getToolsForQuery("hi", listOf(readWriteConfig)).tools.isEmpty())
        assertTrue(router.getToolsForQuery("hello there", listOf(readWriteConfig)).tools.isEmpty())
        assertTrue(router.getToolsForQuery("thanks", listOf(readWriteConfig)).tools.isEmpty())
    }

    // --- Mixed local + MCP tests ---

    @Test
    fun `SHOULD prefer local tools and disable overlapping MCP tools by default`() {
        val local = listOf(localTool("list_job_templates"))
        val mcp = listOf(
            mcpTool("controller.job_templates_list"),
            mcpTool("controller.users_list")
        )
        router.registerLocalTools(local)
        router.registerMcpTools(mcp)

        val result = router.getToolsForQuery("show my job templates and users").tools
        val names = result.map { it.spec.name }

        assertTrue("list_job_templates" in names)
        assertFalse("controller.job_templates_list" in names)
        assertTrue("controller.users_list" in names)
    }

    @Test
    fun `SHOULD return both local and MCP tools WHEN no overlap`() {
        val local = listOf(localTool("list_hosts"))
        val mcp = listOf(mcpTool("controller.users_list"))

        router.registerLocalTools(local)
        router.registerMcpTools(mcp)

        val result = router.getToolsForQuery("show hosts and users").tools
        val names = result.map { it.spec.name }

        assertTrue("list_hosts" in names)
        assertTrue("controller.users_list" in names)
    }

    // --- Phase 2 local tool tests ---

    @Test
    fun `SHOULD select monitoring tools WHEN query mentions instances`() {
        val tools = listOf(
            localTool("list_instances"),
            localTool("get_instance"),
            localTool("list_instance_groups"),
            localTool("ping"),
            localTool("get_mesh_topology"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show cluster instances").tools
        val names = result.map { it.spec.name }

        assertTrue("list_instances" in names)
        assertTrue("get_instance" in names)
        assertTrue("list_instance_groups" in names)
        assertTrue("ping" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD select monitoring tools WHEN query mentions health`() {
        val tools = listOf(
            localTool("list_instances"),
            localTool("ping"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("check system health").tools
        val names = result.map { it.spec.name }

        assertTrue("list_instances" in names)
        assertTrue("ping" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD select credential tools WHEN query mentions credentials`() {
        val tools = listOf(
            localTool("list_credentials"),
            localTool("get_credential"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show my credentials").tools
        val names = result.map { it.spec.name }

        assertTrue("list_credentials" in names)
        assertTrue("get_credential" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD select project tools WHEN query mentions projects`() {
        val tools = listOf(
            localTool("list_projects"),
            localTool("get_project"),
            localTool("list_execution_environments"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("list projects").tools
        val names = result.map { it.spec.name }

        assertTrue("list_projects" in names)
        assertTrue("get_project" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD select EDA activation tools WHEN query mentions activations`() {
        val tools = listOf(
            localTool("list_eda_activations"),
            localTool("get_eda_activation"),
            localTool("list_eda_audit_rules"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show eda activations").tools
        val names = result.map { it.spec.name }

        assertTrue("list_eda_activations" in names)
        assertTrue("get_eda_activation" in names)
        assertTrue("list_eda_audit_rules" in names)
        assertFalse("list_hosts" in names)
    }

    @Test
    fun `SHOULD auto-disable Phase 2 MCP overlaps WHEN local tools registered`() {
        val localTools = listOf(
            localTool("list_instances"),
            localTool("ping"),
            localTool("list_credentials"),
            localTool("list_projects"),
            localTool("list_eda_activations")
        )
        router.registerLocalTools(localTools)

        assertFalse(router.isToolEnabled("controller.instances_list", ToolSource.MCP))
        assertFalse(router.isToolEnabled("controller.ping_read", ToolSource.MCP))
        assertFalse(router.isToolEnabled("controller.credentials_list", ToolSource.MCP))
        assertFalse(router.isToolEnabled("controller.projects_list", ToolSource.MCP))
        assertFalse(router.isToolEnabled("eda.activations_list", ToolSource.MCP))
        assertTrue(router.isToolEnabled("controller.users_list", ToolSource.MCP))
    }

    @Test
    fun `SHOULD rank list tools above write tools for budget cuts`() {
        val tools = listOf(
            localTool("launch_job", destructive = true),
            localTool("get_job_stdout"),
            localTool("list_job_templates"),
            localTool("get_job"),
            localTool("list_jobs"),
            localTool("list_workflow_templates"),
            localTool("launch_workflow", destructive = true),
            localTool("get_workflow_job"),
            localTool("list_schedules"),
            localTool("toggle_schedule", destructive = true)
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("what job templates are available").tools

        assertTrue(result.isNotEmpty())
        val top5 = result.take(5).map { it.spec.name }
        assertTrue("list_job_templates" in top5)
        assertTrue("list_jobs" in top5)
    }

    @Test
    fun `SHOULD boost query-matching tools in ranking`() {
        val tools = listOf(
            localTool("list_schedules"),
            localTool("toggle_schedule", destructive = true),
            localTool("list_job_templates"),
            localTool("launch_job", destructive = true),
            localTool("get_job"),
            localTool("list_jobs")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show my schedules").tools
        val names = result.map { it.spec.name }

        assertEquals("list_schedules", names[0])
    }

    @Test
    fun `SHOULD select monitoring tools WHEN query mentions mesh topology`() {
        val tools = listOf(
            localTool("get_mesh_topology"),
            localTool("list_instances"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)
        val result = router.getToolsForQuery("show mesh topology").tools
        val names = result.map { it.spec.name }

        assertTrue("get_mesh_topology" in names)
        assertTrue("list_instances" in names)
        assertFalse("list_hosts" in names)
    }

    // --- Stemming tests ---

    @Test
    fun `SHOULD match plurals via stemming - orgs matches USERS`() {
        router.registerLocalTools(emptyList())
        router.registerMcpTools(listOf(mcpTool("controller.organizations_list")))

        val result = router.getToolsForQuery("list orgs").tools
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.spec.name == "controller.organizations_list" })
    }

    @Test
    fun `SHOULD match plurals via stemming - creds matches SECURITY`() {
        val tools = listOf(localTool("list_credentials"), localTool("get_credential"))
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show my creds").tools
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `SHOULD match plurals via stemming - inventories matches INVENTORY`() {
        val tools = listOf(localTool("list_inventories"), localTool("list_hosts"))
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show inventories").tools
        val names = result.map { it.spec.name }

        assertTrue("list_inventories" in names)
    }

    // --- Stemmer guard tests ---

    @Test
    fun `stem SHOULD return original WHEN result would be single char or empty`() {
        assertEquals("de", ToolRouter.stem("de"))
        assertEquals("ee", ToolRouter.stem("ee"))
        assertEquals("des", ToolRouter.stem("des"))
        assertEquals("ees", ToolRouter.stem("ees"))
    }

    @Test
    fun `stem SHOULD allow 2-char results`() {
        assertEquals("jt", ToolRouter.stem("jts"))
        assertEquals("up", ToolRouter.stem("up"))
    }

    @Test
    fun `stem SHOULD work normally WHEN result is 3 or more chars`() {
        assertEquals("org", ToolRouter.stem("orgs"))
        assertEquals("host", ToolRouter.stem("hosts"))
        assertEquals("credential", ToolRouter.stem("credentials"))
        assertEquals("inventory", ToolRouter.stem("inventories"))
    }

    // --- New keyword tests ---

    @Test
    fun `SHOULD match JOBS via abbreviation jt`() {
        val tools = listOf(
            localTool("list_job_templates"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show jt").tools
        assertTrue(result.any { it.spec.name == "list_job_templates" })
    }

    @Test
    fun `SHOULD match USERS via abbreviation rbac`() {
        router.registerMcpTools(listOf(
            mcpTool("controller.roles_list"),
            mcpTool("controller.hosts_list")
        ))

        val result = router.getToolsForQuery("check rbac roles").tools
        assertTrue(result.any { it.spec.name == "controller.roles_list" })
    }

    @Test
    fun `SHOULD match MONITORING via keyword up`() {
        val tools = listOf(
            localTool("list_instances"),
            localTool("ping"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("is AAP up?").tools
        assertTrue(result.isNotEmpty())
        assertFalse(result.any { it.spec.name == "list_hosts" })
    }

    @Test
    fun `SHOULD match JOBS via keyword error`() {
        val tools = listOf(
            localTool("list_jobs"),
            localTool("get_job"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("any errors?").tools
        assertTrue(result.isNotEmpty())
        assertFalse(result.any { it.spec.name == "list_hosts" })
    }

    @Test
    fun `SHOULD match EDA via keyword de for decision environment`() {
        val tools = listOf(
            localTool("list_eda_activations"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show de").tools
        assertTrue(result.any { it.spec.name == "list_eda_activations" })
    }

    @Test
    fun `SHOULD match CONFIGURATION via keyword ee for execution environment`() {
        val tools = listOf(
            localTool("list_execution_environments"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("list ee").tools
        assertTrue(result.any { it.spec.name == "list_execution_environments" })
    }

    @Test
    fun `SHOULD match CONFIGURATION via keyword scm`() {
        val tools = listOf(
            localTool("list_projects"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show scm projects").tools
        assertTrue(result.any { it.spec.name == "list_projects" })
    }

    @Test
    fun `SHOULD match INVENTORY via keyword facts`() {
        val tools = listOf(
            localTool("get_host_facts"),
            localTool("list_hosts"),
            localTool("list_jobs")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("gather facts").tools
        val names = result.map { it.spec.name }

        assertTrue("get_host_facts" in names)
        assertFalse("list_jobs" in names)
    }

    // --- Dual-category keyword tests ---

    @Test
    fun `SHOULD match both JOBS and MONITORING via keyword status`() {
        val tools = listOf(
            localTool("list_jobs"),
            localTool("get_job"),
            localTool("list_instances"),
            localTool("ping"),
            localTool("list_hosts")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("what is the status").tools
        val names = result.map { it.spec.name }

        assertTrue("list_jobs" in names || "get_job" in names)
        assertTrue("list_instances" in names || "ping" in names)
    }

    @Test
    fun `SHOULD match both INVENTORY and MONITORING via keyword group`() {
        val tools = listOf(
            localTool("list_hosts"),
            localTool("list_inventories"),
            localTool("list_instance_groups")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show groups").tools
        val names = result.map { it.spec.name }

        assertTrue(names.size >= 2)
    }

    // --- Cherry-pick tests ---

    @Test
    fun `SHOULD cherry-pick relevant tools from matched category`() {
        val tools = listOf(
            localTool("list_job_templates"),
            localTool("launch_job", destructive = true),
            localTool("get_job"),
            localTool("get_job_stdout"),
            localTool("list_jobs"),
            localTool("list_workflow_templates"),
            localTool("launch_workflow", destructive = true),
            localTool("get_workflow_job"),
            localTool("list_schedules"),
            localTool("toggle_schedule", destructive = true)
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show my schedules").tools
        val names = result.map { it.spec.name }

        assertTrue("list_schedules" in names)
        assertTrue("toggle_schedule" in names)
    }

    @Test
    fun `SHOULD cherry-pick MCP tools by name overlap`() {
        val tools = listOf(
            mcpTool("controller.users_list"),
            mcpTool("controller.users_read"),
            mcpTool("controller.teams_list"),
            mcpTool("controller.organizations_list"),
            mcpTool("controller.tokens_list"),
            mcpTool("controller.roles_list")
        )
        router.registerMcpTools(tools)

        val result = router.getToolsForQuery("list users", listOf(readWriteConfig)).tools
        val names = result.map { it.spec.name }

        assertTrue("controller.users_list" in names)
        assertTrue("controller.users_read" in names)
    }

    @Test
    fun `SHOULD fallback to list tools WHEN no cherry-pick overlap`() {
        val tools = listOf(
            localTool("list_instances"),
            localTool("get_instance"),
            localTool("list_instance_groups"),
            localTool("ping"),
            localTool("get_mesh_topology")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("is everything healthy?").tools
        val names = result.map { it.spec.name }

        assertTrue("list_instances" in names)
        assertTrue("list_instance_groups" in names)
        assertTrue("ping" in names)
    }

    @Test
    fun `SHOULD cherry-pick audit rules as EDA not SECURITY`() {
        val tools = listOf(
            localTool("list_eda_audit_rules"),
            localTool("list_credentials"),
            localTool("get_credential")
        )
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("show audit rules").tools
        val names = result.map { it.spec.name }

        assertTrue("list_eda_audit_rules" in names)
    }

    // --- Stop words tests ---

    @Test
    fun `SHOULD ignore expanded stop words in matching`() {
        val tools = listOf(localTool("list_hosts"), localTool("list_jobs"))
        router.registerLocalTools(tools)

        val result = router.getToolsForQuery("for any of the been").tools
        assertTrue(result.isEmpty())
    }

    @Test
    fun `categoryMatched SHOULD be false WHEN only stop words in query`() {
        router.registerLocalTools(listOf(localTool("list_hosts")))
        val result = router.getToolsForQuery("show me all of the")
        assertFalse(result.categoryMatched)
    }
}
