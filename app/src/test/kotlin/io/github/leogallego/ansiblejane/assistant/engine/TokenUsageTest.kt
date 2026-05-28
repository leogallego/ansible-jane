package io.github.leogallego.ansiblejane.assistant.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TokenUsageFormatTest(
    private val totalTokens: Int,
    private val isEstimated: Boolean,
    private val expected: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "total={0}, estimated={1} -> \"{2}\"")
        fun data() = listOf(
            arrayOf<Any>(0, false, "0"),
            arrayOf<Any>(1, false, "1"),
            arrayOf<Any>(500, false, "500"),
            arrayOf<Any>(999, false, "999"),
            arrayOf<Any>(1000, false, "1K"),
            arrayOf<Any>(1099, false, "1K"),
            arrayOf<Any>(1100, false, "1.1K"),
            arrayOf<Any>(2000, false, "2K"),
            arrayOf<Any>(2050, false, "2K"),
            arrayOf<Any>(2400, false, "2.4K"),
            arrayOf<Any>(10000, false, "10K"),
            arrayOf<Any>(12300, false, "12.3K"),
            arrayOf<Any>(100000, false, "100K"),
            arrayOf<Any>(0, true, "~0"),
            arrayOf<Any>(500, true, "~500"),
            arrayOf<Any>(1200, true, "~1.2K"),
            arrayOf<Any>(2000, true, "~2K"),
        )
    }

    @Test
    fun `formatTotal produces expected output`() {
        val usage = TokenUsage(0, 0, totalTokens, isEstimated)
        assertEquals(expected, usage.formatTotal())
    }

    @Test
    fun `formatTokenCount matches formatTotal`() {
        assertEquals(
            TokenUsage(0, 0, totalTokens, isEstimated).formatTotal(),
            TokenUsage.formatTokenCount(totalTokens, isEstimated)
        )
    }
}
