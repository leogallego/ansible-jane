package io.github.leogallego.ansiblejane.assistant.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenUsageFormatTest {

    private fun assertFormat(totalTokens: Int, isEstimated: Boolean, expected: String) {
        val usage = TokenUsage(0, 0, totalTokens, isEstimated)
        assertEquals(expected, usage.formatTotal())
        assertEquals(expected, TokenUsage.formatTokenCount(totalTokens, isEstimated))
    }

    @Test fun `formatTotal 0 exact`() = assertFormat(0, false, "0")
    @Test fun `formatTotal 1 exact`() = assertFormat(1, false, "1")
    @Test fun `formatTotal 500 exact`() = assertFormat(500, false, "500")
    @Test fun `formatTotal 999 exact`() = assertFormat(999, false, "999")
    @Test fun `formatTotal 1000 exact`() = assertFormat(1000, false, "1K")
    @Test fun `formatTotal 1099 exact`() = assertFormat(1099, false, "1K")
    @Test fun `formatTotal 1100 exact`() = assertFormat(1100, false, "1.1K")
    @Test fun `formatTotal 2000 exact`() = assertFormat(2000, false, "2K")
    @Test fun `formatTotal 2050 exact`() = assertFormat(2050, false, "2K")
    @Test fun `formatTotal 2400 exact`() = assertFormat(2400, false, "2.4K")
    @Test fun `formatTotal 10000 exact`() = assertFormat(10000, false, "10K")
    @Test fun `formatTotal 12300 exact`() = assertFormat(12300, false, "12.3K")
    @Test fun `formatTotal 100000 exact`() = assertFormat(100000, false, "100K")
    @Test fun `formatTotal 0 estimated`() = assertFormat(0, true, "~0")
    @Test fun `formatTotal 500 estimated`() = assertFormat(500, true, "~500")
    @Test fun `formatTotal 1200 estimated`() = assertFormat(1200, true, "~1.2K")
    @Test fun `formatTotal 2000 estimated`() = assertFormat(2000, true, "~2K")
}
