package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSource
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToolRouterTest {

    private lateinit var router: ToolRouter

    private fun localTool(name: String, destructive: Boolean = false) = object : LocalTool(
        spec = ToolSpec(name, "Local: $name", JsonObject(emptyMap())),
        destructive = destructive
    ) {
        override suspend fun execute(args: Map<String, Any>) = ToolResult(success = true)
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
        val result = router.getToolsForQuery("list my hosts")
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
        val result = router.getToolsForQuery("launch a job template")
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
        val result = router.getToolsForQuery("hello how are you")
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

        assertTrue(router.getToolsForQuery("hi").isEmpty())
        assertTrue(router.getToolsForQuery("hello there").isEmpty())
        assertTrue(router.getToolsForQuery("what can you do?").isEmpty())
        assertTrue(router.getToolsForQuery("thanks").isEmpty())
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
        val result = router.getToolsForQuery("show hosts running failed jobs")
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
        val result = router.getToolsForQuery("show eda audit rules")
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

        val result = router.getToolsForQuery("show my hosts and inventories")
        val names = result.map { it.spec.name }

        assertFalse("list_hosts" in names)
        assertTrue("list_inventories" in names)
    }

    @Test
    fun `SHOULD return empty WHEN no tools registered at all`() {
        val result = router.getToolsForQuery("list hosts")
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
        val result = router.getToolsForQuery("show my workflow templates")
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
        val result = router.getToolsForQuery("show my schedules")
        val names = result.map { it.spec.name }

        assertTrue("list_schedules" in names)
        assertTrue("toggle_schedule" in names)
        assertFalse("list_hosts" in names)
    }
}
