package io.github.leogallego.ansiblejane.data.backup

import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private val manager = BackupManager()

    private val testInstances = listOf(
        AapInstance(
            id = "inst-1",
            baseUrl = "https://aap.example.com",
            token = "secret-token-123",
            alias = "Production",
            apiVersion = "CONTROLLER_V2",
            trustSelfSigned = false,
            mcpServerUrls = listOf(
                McpServerConfig(url = "https://aap.example.com:8448/mcp", label = "aap")
            ),
            mcpEnabled = true
        ),
        AapInstance(
            id = "inst-2",
            baseUrl = "https://staging.example.com",
            token = "staging-token-456",
            alias = "Staging",
            apiVersion = "CONTROLLER_V2",
            trustSelfSigned = true
        )
    )

    @Test
    fun `round-trip export and import preserves all instance data`() {
        val exported = manager.exportBackup("mypassword", testInstances)
        val envelope = manager.importBackup(exported, "mypassword")

        assertEquals(2, envelope.instances.size)

        val prod = envelope.instances.find { it.id == "inst-1" }!!
        assertEquals("https://aap.example.com", prod.baseUrl)
        assertEquals("secret-token-123", prod.token)
        assertEquals("Production", prod.alias)
        assertEquals(true, prod.mcpEnabled)
        assertEquals(1, prod.mcpServerUrls?.size)

        val staging = envelope.instances.find { it.id == "inst-2" }!!
        assertEquals("https://staging.example.com", staging.baseUrl)
        assertEquals("staging-token-456", staging.token)
        assertEquals(true, staging.trustSelfSigned)
    }

    @Test
    fun `round-trip preserves LLM config when included`() {
        val llmConfig = LlmProviderConfig.OpenAiCompatible(
            url = "https://openrouter.ai/api/v1",
            model = "anthropic/claude-sonnet-4-6",
            apiKey = "sk-or-key-123",
            tokenSavingMode = TokenSavingMode.TOKEN_SAVER
        )
        val exported = manager.exportBackup("pass", testInstances, llmConfig)
        val envelope = manager.importBackup(exported, "pass")

        assertNotNull(envelope.llmConfig)
        val config = envelope.llmConfig as LlmProviderConfig.OpenAiCompatible
        assertEquals("https://openrouter.ai/api/v1", config.url)
        assertEquals("anthropic/claude-sonnet-4-6", config.model)
        assertEquals("sk-or-key-123", config.apiKey)
        assertEquals(TokenSavingMode.TOKEN_SAVER, config.tokenSavingMode)
    }

    @Test
    fun `export without LLM config leaves it null`() {
        val exported = manager.exportBackup("pass", testInstances)
        val envelope = manager.importBackup(exported, "pass")
        assertNull(envelope.llmConfig)
    }

    @Test(expected = BackupDecryptionException::class)
    fun `wrong password throws BackupDecryptionException`() {
        val exported = manager.exportBackup("correct", testInstances)
        manager.importBackup(exported, "wrong")
    }

    @Test(expected = BackupDecryptionException::class)
    fun `corrupted data throws BackupDecryptionException`() {
        manager.importBackup(ByteArray(100) { 0x42 }, "password")
    }

    @Test(expected = BackupDecryptionException::class)
    fun `truncated data throws BackupDecryptionException`() {
        manager.importBackup(ByteArray(10), "password")
    }

    @Test
    fun `exported data is not readable as plaintext`() {
        val exported = manager.exportBackup("pass", testInstances)
        val asString = String(exported, Charsets.UTF_8)
        assertTrue(!asString.contains("secret-token-123"))
        assertTrue(!asString.contains("aap.example.com"))
    }

    @Test
    fun `different passwords produce different ciphertexts`() {
        val export1 = manager.exportBackup("password1", testInstances)
        val export2 = manager.exportBackup("password2", testInstances)
        assertTrue(!export1.contentEquals(export2))
    }

    @Test
    fun `same password produces different ciphertexts due to random salt`() {
        val export1 = manager.exportBackup("same", testInstances)
        val export2 = manager.exportBackup("same", testInstances)
        assertTrue(!export1.contentEquals(export2))
        // But both decrypt to the same data
        val env1 = manager.importBackup(export1, "same")
        val env2 = manager.importBackup(export2, "same")
        assertEquals(env1.instances.size, env2.instances.size)
    }

    @Test
    fun `envelope version is set to 1`() {
        val exported = manager.exportBackup("pass", testInstances)
        val envelope = manager.importBackup(exported, "pass")
        assertEquals(1, envelope.version)
    }

    @Test
    fun `envelope has valid timestamp`() {
        val before = System.currentTimeMillis()
        val exported = manager.exportBackup("pass", testInstances)
        val after = System.currentTimeMillis()
        val envelope = manager.importBackup(exported, "pass")
        assertTrue(envelope.createdAt in before..after)
    }
}
