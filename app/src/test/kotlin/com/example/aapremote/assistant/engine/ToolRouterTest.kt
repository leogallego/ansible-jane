package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.model.McpServerConfig
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRouterTest {

    private fun tool(name: String, serverLabel: String = "aap") = object : Tool {
        override val spec = ToolSpec(name, "[$serverLabel] description of $name", JsonObject(emptyMap()))
        override suspend fun execute(args: Map<String, Any>) = ToolResult(success = true)
    }

    private val readOnlyConfig = McpServerConfig(url = "https://aap:8448/mcp", label = "aap", readOnly = true)
    private val readWriteConfig = McpServerConfig(url = "https://aap:8448/mcp", label = "aap", readOnly = false)

    @Test
    fun `SHOULD exclude write tools WHEN readOnly is true GIVEN mixed read and write tools`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.hosts_create"),
            tool("controller.hosts_update"),
            tool("controller.hosts_delete"),
            tool("controller.inventories_list")
        )
        val result = ToolRouter.filterTools("list my hosts", tools, listOf(readOnlyConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.hosts_list" in names)
        assertTrue("controller.inventories_list" in names)
        assertTrue("controller.hosts_create" !in names)
        assertTrue("controller.hosts_update" !in names)
        assertTrue("controller.hosts_delete" !in names)
    }

    @Test
    fun `SHOULD keep all tools WHEN readOnly is false GIVEN write tools present`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.hosts_create"),
            tool("controller.hosts_delete")
        )
        val result = ToolRouter.filterTools("list hosts", tools, listOf(readWriteConfig))
        assertEquals(tools.size, result.size)
    }

    @Test
    fun `SHOULD exclude launch and cancel actions WHEN readOnly is true GIVEN job tools`() {
        val tools = listOf(
            tool("controller.jobs_read"),
            tool("controller.job_templates_launch"),
            tool("controller.jobs_relaunch"),
            tool("controller.jobs_cancel")
        )
        val result = ToolRouter.filterTools("show me jobs", tools, listOf(readOnlyConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.jobs_read" in names)
        assertTrue("controller.job_templates_launch" !in names)
        assertTrue("controller.jobs_relaunch" !in names)
        assertTrue("controller.jobs_cancel" !in names)
    }

    @Test
    fun `SHOULD only filter matching server label WHEN multiple servers configured GIVEN mixed servers`() {
        val aapWriteTool = tool("controller.hosts_create", "aap")
        val aapReadTool = tool("controller.hosts_list", "aap")
        val otherWriteTool = tool("controller.hosts_create", "knowledge")
        val tools = listOf(aapWriteTool, aapReadTool, otherWriteTool)

        val configs = listOf(
            McpServerConfig(url = "https://aap:8448/mcp", label = "aap", readOnly = true),
            McpServerConfig(url = "https://kb:3000/mcp", label = "knowledge", readOnly = false)
        )
        val result = ToolRouter.filterTools("list hosts", tools, configs)
        val names = result.map { it.spec.name to it.spec.description }

        assertTrue(names.any { it.first == "controller.hosts_list" && it.second.contains("[aap]") })
        assertTrue(names.none { it.first == "controller.hosts_create" && it.second.contains("[aap]") })
        assertTrue(names.any { it.first == "controller.hosts_create" && it.second.contains("[knowledge]") })
    }

    @Test
    fun `SHOULD select inventory tools WHEN query mentions hosts GIVEN mixed category tools`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.groups_list"),
            tool("controller.jobs_read"),
            tool("controller.users_list")
        )
        val result = ToolRouter.filterTools("list my hosts", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.hosts_list" in names)
        assertTrue("controller.groups_list" in names)
        assertTrue("controller.jobs_read" !in names)
        assertTrue("controller.users_list" !in names)
    }

    @Test
    fun `SHOULD select job tools WHEN query mentions launch and template GIVEN mixed category tools`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.job_templates_list"),
            tool("controller.job_templates_launch"),
            tool("controller.jobs_read")
        )
        val result = ToolRouter.filterTools("launch a job template", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.job_templates_list" in names)
        assertTrue("controller.job_templates_launch" in names)
        assertTrue("controller.jobs_read" in names)
        assertTrue("controller.hosts_list" !in names)
    }

    @Test
    fun `SHOULD include tools from all matched categories WHEN query spans multiple categories GIVEN mixed tools`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.jobs_read"),
            tool("controller.users_list"),
            tool("controller.credentials_list")
        )
        val result = ToolRouter.filterTools("show hosts running failed jobs", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.hosts_list" in names)
        assertTrue("controller.jobs_read" in names)
        assertTrue("controller.users_list" !in names)
    }

    @Test
    fun `SHOULD return no tools WHEN no category matches GIVEN generic query`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.jobs_read"),
            tool("controller.users_list")
        )
        val result = ToolRouter.filterTools("hello how are you", tools, listOf(readWriteConfig))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `SHOULD apply both readOnly and category filters WHEN readOnly is true and category matches GIVEN mixed tools`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.hosts_create"),
            tool("controller.hosts_delete"),
            tool("controller.jobs_read"),
            tool("controller.inventories_list")
        )
        val result = ToolRouter.filterTools("list my hosts", tools, listOf(readOnlyConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.hosts_list" in names)
        assertTrue("controller.inventories_list" in names)
        assertTrue("controller.hosts_create" !in names)
        assertTrue("controller.hosts_delete" !in names)
        assertTrue("controller.jobs_read" !in names)
    }

    @Test
    fun `SHOULD return empty list WHEN tool list is empty GIVEN any query`() {
        val result = ToolRouter.filterTools("list hosts", emptyList(), listOf(readOnlyConfig))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `SHOULD parse compound resource names correctly WHEN query matches compound resource GIVEN compound tool names`() {
        val tools = listOf(
            tool("controller.workflow_job_templates_list"),
            tool("controller.constructed_inventories_list"),
            tool("controller.hosts_list")
        )
        val result = ToolRouter.filterTools("show my workflow templates", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.workflow_job_templates_list" in names)
        assertTrue("controller.hosts_list" !in names)
    }

    @Test
    fun `SHOULD not apply readOnly filtering WHEN serverConfigs is empty GIVEN category-matched write tools`() {
        val tools = listOf(
            tool("controller.hosts_create"),
            tool("controller.hosts_delete")
        )
        val result = ToolRouter.filterTools("list hosts", tools, emptyList())
        assertEquals(tools.size, result.size)
    }

    @Test
    fun `SHOULD return no tools WHEN query is a greeting GIVEN all tools available`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.jobs_read"),
            tool("controller.users_list"),
            tool("controller.credentials_list"),
            tool("controller.settings_read")
        )
        assertTrue(ToolRouter.filterTools("hi", tools, listOf(readWriteConfig)).isEmpty())
        assertTrue(ToolRouter.filterTools("hello there", tools, listOf(readWriteConfig)).isEmpty())
        assertTrue(ToolRouter.filterTools("what can you do?", tools, listOf(readWriteConfig)).isEmpty())
        assertTrue(ToolRouter.filterTools("thanks", tools, listOf(readWriteConfig)).isEmpty())
    }

    @Test
    fun `SHOULD select monitoring tools WHEN query mentions health or status GIVEN mixed tools`() {
        val tools = listOf(
            tool("controller.hosts_list"),
            tool("controller.instances_list"),
            tool("controller.ping_read"),
            tool("controller.dashboard_read")
        )
        val result = ToolRouter.filterTools("check system health", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.instances_list" in names)
        assertTrue("controller.ping_read" in names)
        assertTrue("controller.dashboard_read" in names)
        assertTrue("controller.hosts_list" !in names)
    }

    @Test
    fun `SHOULD select user tools WHEN query mentions users or teams GIVEN mixed tools`() {
        val tools = listOf(
            tool("controller.users_list"),
            tool("controller.teams_list"),
            tool("controller.organizations_list"),
            tool("controller.hosts_list")
        )
        val result = ToolRouter.filterTools("list users in my team", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.users_list" in names)
        assertTrue("controller.teams_list" in names)
        assertTrue("controller.organizations_list" in names)
        assertTrue("controller.hosts_list" !in names)
    }

    @Test
    fun `SHOULD select security tools WHEN query mentions credentials GIVEN mixed tools`() {
        val tools = listOf(
            tool("controller.credentials_list"),
            tool("controller.credential_types_list"),
            tool("controller.hosts_list"),
            tool("controller.jobs_read")
        )
        val result = ToolRouter.filterTools("show my credentials", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.credentials_list" in names)
        assertTrue("controller.credential_types_list" in names)
        assertTrue("controller.hosts_list" !in names)
        assertTrue("controller.jobs_read" !in names)
    }

    @Test
    fun `SHOULD select configuration tools WHEN query mentions settings or projects GIVEN mixed tools`() {
        val tools = listOf(
            tool("controller.settings_read"),
            tool("controller.projects_list"),
            tool("controller.notification_templates_list"),
            tool("controller.hosts_list")
        )
        val result = ToolRouter.filterTools("show project settings", tools, listOf(readWriteConfig))
        val names = result.map { it.spec.name }

        assertTrue("controller.settings_read" in names)
        assertTrue("controller.projects_list" in names)
        assertTrue("controller.notification_templates_list" in names)
        assertTrue("controller.hosts_list" !in names)
    }

    @Test
    fun `SHOULD match each category keyword WHEN single keyword used GIVEN inventory tools`() {
        val tools = listOf(tool("controller.hosts_list"), tool("controller.jobs_read"))

        assertTrue(ToolRouter.filterTools("show host", tools, listOf(readWriteConfig)).any { it.spec.name.contains("hosts") })
        assertTrue(ToolRouter.filterTools("list groups", tools, listOf(readWriteConfig)).any { it.spec.name.contains("hosts") })
        assertTrue(ToolRouter.filterTools("check inventory", tools, listOf(readWriteConfig)).any { it.spec.name.contains("hosts") })
        assertTrue(ToolRouter.filterTools("show infrastructure", tools, listOf(readWriteConfig)).any { it.spec.name.contains("hosts") })
    }
}
