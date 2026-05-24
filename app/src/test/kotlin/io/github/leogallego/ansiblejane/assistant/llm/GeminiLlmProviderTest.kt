package io.github.leogallego.ansiblejane.assistant.llm

import ai.koog.http.client.KoogHttpClientException
import ai.koog.prompt.executor.clients.LLMClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class GeminiLlmProviderTest {

    private val provider = GeminiLlmProvider(apiKey = "test-key", modelId = "gemini-2.5-flash")

    @Test
    fun `SHOULD throw LlmAuthException WHEN KoogHttpClientException with 401`() {
        val ex = KoogHttpClientException(statusCode = 401, message = "Unauthorized")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmAuthException)
        assertTrue(mapped.message!!.contains("Authentication failed"))
    }

    @Test
    fun `SHOULD throw LlmAuthException WHEN KoogHttpClientException with 403`() {
        val ex = KoogHttpClientException(statusCode = 403, message = "Forbidden")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmAuthException)
        assertTrue(mapped.message!!.contains("Authentication failed"))
    }

    @Test
    fun `SHOULD throw LlmRateLimitException WHEN KoogHttpClientException with 429`() {
        val ex = KoogHttpClientException(statusCode = 429, message = "Rate limited")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmRateLimitException)
        assertTrue(mapped.message!!.contains("Rate limited"))
    }

    @Test
    fun `SHOULD throw LlmServerException WHEN KoogHttpClientException with 500`() {
        val ex = KoogHttpClientException(statusCode = 500, message = "Internal error")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD throw LlmServerException WHEN KoogHttpClientException with 502`() {
        val ex = KoogHttpClientException(statusCode = 502, message = "Bad gateway")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD throw LlmServerException WHEN KoogHttpClientException with 503`() {
        val ex = KoogHttpClientException(statusCode = 503, message = "Service unavailable")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD throw LlmServerException WHEN KoogHttpClientException with 504`() {
        val ex = KoogHttpClientException(statusCode = 504, message = "Gateway timeout")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD throw LlmServerException WHEN KoogHttpClientException with no status code`() {
        val ex = KoogHttpClientException(message = "Connection failed")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD throw LlmServerException WHEN KoogHttpClientException with client error`() {
        val ex = KoogHttpClientException(statusCode = 400, message = "Bad request")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD throw LlmTimeoutException WHEN SocketTimeoutException`() {
        val ex = SocketTimeoutException("timed out")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmTimeoutException)
    }

    @Test
    fun `SHOULD unwrap LLMClientException WHEN cause is KoogHttpClientException 429`() {
        val inner = KoogHttpClientException(statusCode = 429, message = "Rate limited")
        val ex = LLMClientException("google", "LLM error", inner)
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmRateLimitException)
    }

    @Test
    fun `SHOULD unwrap LLMClientException WHEN cause is KoogHttpClientException 401`() {
        val inner = KoogHttpClientException(statusCode = 401, message = "Bad key")
        val ex = LLMClientException("google", "LLM error", inner)
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmAuthException)
    }

    @Test
    fun `SHOULD unwrap LLMClientException WHEN cause is SocketTimeoutException`() {
        val inner = SocketTimeoutException("Request timed out")
        val ex = LLMClientException("google", "LLM timeout", inner)
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmTimeoutException)
    }

    @Test
    fun `SHOULD return LlmServerException WHEN LLMClientException has no cause`() {
        val ex = LLMClientException("google", "Unknown error")
        val mapped = provider.mapException(ex)
        assertTrue(mapped is LlmServerException)
    }

    @Test
    fun `SHOULD pass through already-mapped LlmAuthException`() {
        val ex = LlmAuthException("Already mapped")
        val mapped = provider.mapException(ex)
        assertTrue(mapped === ex)
    }

    @Test
    fun `SHOULD pass through already-mapped LlmRateLimitException`() {
        val ex = LlmRateLimitException("Already mapped")
        val mapped = provider.mapException(ex)
        assertTrue(mapped === ex)
    }

    @Test
    fun `SHOULD pass through already-mapped LlmServerException`() {
        val ex = LlmServerException("Already mapped")
        val mapped = provider.mapException(ex)
        assertTrue(mapped === ex)
    }

    @Test
    fun `SHOULD pass through already-mapped LlmTimeoutException`() {
        val ex = LlmTimeoutException("Already mapped")
        val mapped = provider.mapException(ex)
        assertTrue(mapped === ex)
    }

    @Test
    fun `SHOULD return original exception WHEN unknown type`() {
        val ex = IllegalStateException("Something weird")
        val mapped = provider.mapException(ex)
        assertTrue(mapped === ex)
    }

    @Test
    fun `SHOULD return true from isAvailable`() {
        assertTrue(provider.isAvailable())
    }

    @Test
    fun `SHOULD return correct model info`() {
        val info = provider.modelInfo()
        assertEquals("gemini-2.5-flash", info.name)
        assertEquals(false, info.isLocal)
    }
}
