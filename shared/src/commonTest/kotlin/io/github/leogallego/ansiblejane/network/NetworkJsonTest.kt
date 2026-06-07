package io.github.leogallego.ansiblejane.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Serializable
private data class Sample(val known: String, val optional: String? = null)

class NetworkJsonTest {

    @Test
    fun ignoresUnknownKeys() {
        val json = """{"known":"v","extra":"ignored"}"""
        val result = networkJson.decodeFromString<Sample>(json)
        assertEquals("v", result.known)
    }

    @Test
    fun omitsExplicitNulls() {
        val encoded = networkJson.encodeToString(Sample(known = "v"))
        assertFalse(encoded.contains("optional"), "Null fields should be omitted")
    }

    @Test
    fun encodesDefaults() {
        val encoded = networkJson.encodeToString(SampleWithDefault(name = "test"))
        assert(encoded.contains("\"active\":true")) { "Defaults should be encoded" }
    }
}

@Serializable
private data class SampleWithDefault(val name: String, val active: Boolean = true)
