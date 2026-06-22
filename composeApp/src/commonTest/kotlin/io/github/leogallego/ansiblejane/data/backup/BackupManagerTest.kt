package io.github.leogallego.ansiblejane.data.backup

import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `round-trip export and import preserves all instance data`() = runTest {
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
    fun `round-trip preserves LLM config when included`() = runTest {
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
    fun `export without LLM config leaves it null`() = runTest {
        val exported = manager.exportBackup("pass", testInstances)
        val envelope = manager.importBackup(exported, "pass")
        assertNull(envelope.llmConfig)
    }

    @Test
    fun `wrong password throws BackupDecryptionException`() = runTest {
        val exported = manager.exportBackup("correct", testInstances)
        assertFailsWith<BackupDecryptionException> {
            manager.importBackup(exported, "wrong")
        }
    }

    @Test
    fun `corrupted data throws BackupDecryptionException`() = runTest {
        assertFailsWith<BackupDecryptionException> {
            manager.importBackup(ByteArray(100) { 0x42 }, "password")
        }
    }

    @Test
    fun `truncated data throws BackupDecryptionException`() = runTest {
        assertFailsWith<BackupDecryptionException> {
            manager.importBackup(ByteArray(10), "password")
        }
    }

    @Test
    fun `exported data is not readable as plaintext`() = runTest {
        val exported = manager.exportBackup("pass", testInstances)
        val asString = exported.decodeToString()
        assertTrue(!asString.contains("secret-token-123"))
        assertTrue(!asString.contains("aap.example.com"))
    }

    @Test
    fun `different passwords produce different ciphertexts`() = runTest {
        val export1 = manager.exportBackup("password1", testInstances)
        val export2 = manager.exportBackup("password2", testInstances)
        assertTrue(!export1.contentEquals(export2))
    }

    @Test
    fun `same password produces different ciphertexts due to random salt`() = runTest {
        val export1 = manager.exportBackup("same", testInstances)
        val export2 = manager.exportBackup("same", testInstances)
        assertTrue(!export1.contentEquals(export2))
        val env1 = manager.importBackup(export1, "same")
        val env2 = manager.importBackup(export2, "same")
        assertEquals(env1.instances.size, env2.instances.size)
    }

    @Test
    fun `envelope version is set to 2`() = runTest {
        val exported = manager.exportBackup("pass", testInstances)
        val envelope = manager.importBackup(exported, "pass")
        assertEquals(2, envelope.version)
    }

    @Test
    fun `envelope has valid timestamp`() = runTest {
        val exported = manager.exportBackup("pass", testInstances)
        val envelope = manager.importBackup(exported, "pass")
        assertTrue(envelope.createdAt > 0)
    }
}
