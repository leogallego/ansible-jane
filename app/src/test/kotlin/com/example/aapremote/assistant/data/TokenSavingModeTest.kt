package com.example.aapremote.assistant.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenSavingModeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SHOULD default to STANDARD WHEN tokenSavingMode is not specified GIVEN OpenAiCompatible config`() {
        val config = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4"
        )
        assertEquals(TokenSavingMode.STANDARD, config.tokenSavingMode)
    }

    @Test
    fun `SHOULD serialize and deserialize STANDARD mode GIVEN config with default mode`() {
        val config = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4",
            tokenSavingMode = TokenSavingMode.STANDARD
        )
        val serialized = json.encodeToString(LlmProviderConfig.serializer(), config)
        val deserialized = json.decodeFromString(LlmProviderConfig.serializer(), serialized)
        assertEquals(config.tokenSavingMode, (deserialized as LlmProviderConfig.OpenAiCompatible).tokenSavingMode)
    }

    @Test
    fun `SHOULD serialize and deserialize TOKEN_SAVER mode GIVEN config with token saver`() {
        val config = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4",
            tokenSavingMode = TokenSavingMode.TOKEN_SAVER
        )
        val serialized = json.encodeToString(LlmProviderConfig.serializer(), config)
        val deserialized = json.decodeFromString(LlmProviderConfig.serializer(), serialized)
        assertEquals(TokenSavingMode.TOKEN_SAVER, (deserialized as LlmProviderConfig.OpenAiCompatible).tokenSavingMode)
    }

    @Test
    fun `SHOULD serialize and deserialize TOOLS_ONLY mode GIVEN config with minimal`() {
        val config = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4",
            tokenSavingMode = TokenSavingMode.TOOLS_ONLY
        )
        val serialized = json.encodeToString(LlmProviderConfig.serializer(), config)
        val deserialized = json.decodeFromString(LlmProviderConfig.serializer(), serialized)
        assertEquals(TokenSavingMode.TOOLS_ONLY, (deserialized as LlmProviderConfig.OpenAiCompatible).tokenSavingMode)
    }

    @Test
    fun `SHOULD default to STANDARD WHEN deserializing legacy config without tokenSavingMode GIVEN old JSON`() {
        val legacyJson = """{"type":"openai_compatible","url":"https://api.openai.com/v1","model":"gpt-4"}"""
        val config = json.decodeFromString(LlmProviderConfig.serializer(), legacyJson) as LlmProviderConfig.OpenAiCompatible
        assertEquals(TokenSavingMode.STANDARD, config.tokenSavingMode)
    }

    @Test
    fun `SHOULD preserve tokenSavingMode WHEN copying config GIVEN config with TOKEN_SAVER`() {
        val original = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4",
            tokenSavingMode = TokenSavingMode.TOKEN_SAVER
        )
        val copy = original.copy(model = "gpt-4o")
        assertEquals(TokenSavingMode.TOKEN_SAVER, copy.tokenSavingMode)
    }

    @Test
    fun `SHOULD have unique display names WHEN iterating all modes GIVEN all entries`() {
        val names = TokenSavingMode.entries.map { it.displayName }
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun `SHOULD have descriptions for all modes GIVEN all entries`() {
        TokenSavingMode.entries.forEach { mode ->
            assert(mode.description.isNotBlank()) { "${mode.name} should have a description" }
        }
    }

    @Test
    fun `SHOULD expose tokenSavingMode via interface WHEN accessed through LlmProviderConfig GIVEN any config`() {
        val config: LlmProviderConfig = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4",
            tokenSavingMode = TokenSavingMode.TOOLS_ONLY
        )
        assertEquals(TokenSavingMode.TOOLS_ONLY, config.tokenSavingMode)
    }

    @Test
    fun `SHOULD preserve apiKey WHEN changing only tokenSavingMode GIVEN full config`() {
        val config = LlmProviderConfig.OpenAiCompatible(
            url = "https://api.openai.com/v1",
            model = "gpt-4",
            apiKey = "sk-test-key",
            tokenSavingMode = TokenSavingMode.STANDARD
        )
        val updated = config.copy(tokenSavingMode = TokenSavingMode.TOOLS_ONLY)
        assertEquals("sk-test-key", updated.apiKey)
        assertEquals(TokenSavingMode.TOOLS_ONLY, updated.tokenSavingMode)
    }
}
