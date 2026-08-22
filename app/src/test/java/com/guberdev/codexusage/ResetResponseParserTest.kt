package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class ResetResponseParserTest {
    @Test
    fun `reset response preserves terminal outcome and reset window count`() {
        assertEquals(
            ResetResponse(ResetOutcome.RESET, 2),
            ResetResponseParser.parse("""{"code":"reset","windows_reset":2}"""),
        )
        assertEquals(
            ResetResponse(ResetOutcome.ALREADY_REDEEMED, 0),
            ResetResponseParser.parse("""{"code":"already_redeemed","windows_reset":0}"""),
        )
    }
}
