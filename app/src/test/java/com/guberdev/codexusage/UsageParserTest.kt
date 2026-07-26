package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageParserTest {
    private val parser = UsageParser()

    @Test
    fun `primary window converts used percent to remaining percent and reset instant`() {
        val snapshot = parser.parse(
            """
            {
              "plan_type": "pro",
              "rate_limit": {
                "allowed": true,
                "limit_reached": false,
                "primary_window": {
                  "used_percent": 45,
                  "limit_window_seconds": 604800,
                  "reset_at": 1785611900
                },
                "secondary_window": null
              },
              "additional_rate_limits": [],
              "credits": {"balance": "0"}
            }
            """.trimIndent(),
        )

        assertEquals(55, snapshot.primary.remainingPercent)
        assertEquals(1785611900L, snapshot.primary.resetAtEpochSeconds)
        assertEquals("pro", snapshot.planType)
        assertEquals("0", snapshot.creditBalance)
    }

    @Test
    fun `decimal used percent rounds remaining to nearest display percent`() {
        val snapshot = parser.parse(
            """
            {
              "plan_type": "plus",
              "rate_limit": {
                "primary_window": {
                  "used_percent": 44.6,
                  "reset_at": 1785611900
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(55, snapshot.primary.remainingPercent)
    }

    @Test
    fun `additional limits are preserved and missing reset is nullable`() {
        val snapshot = parser.parse(
            """
            {
              "plan_type": "pro",
              "rate_limit": {
                "primary_window": {"used_percent": 45, "reset_at": 1785611900}
              },
              "additional_rate_limits": [
                {
                  "metered_feature": "codex_bengalfox",
                  "rate_limit": {
                    "primary_window": {"used_percent": 0}
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, snapshot.additionalLimits.size)
        assertEquals("codex_bengalfox", snapshot.additionalLimits.single().feature)
        assertEquals(100, snapshot.additionalLimits.single().window.remainingPercent)
        assertNull(snapshot.additionalLimits.single().window.resetAtEpochSeconds)
    }

    @Test(expected = UsageParseException::class)
    fun `missing primary window is rejected`() {
        parser.parse("""{"plan_type":"pro","rate_limit":null}""")
    }
}
