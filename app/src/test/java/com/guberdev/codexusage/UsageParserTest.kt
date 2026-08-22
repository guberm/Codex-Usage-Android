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
    fun `spark daily and weekly windows are preserved as remaining percentages`() {
        val snapshot = parser.parse(
            """
            {
              "plan_type": "pro",
              "rate_limit": {
                "primary_window": {"used_percent": 45, "reset_at": 1785611900}
              },
              "additional_rate_limits": [
                {
                  "limit_name": "GPT-5.3-Codex-Spark",
                  "metered_feature": "codex_bengalfox",
                  "rate_limit": {
                    "primary_window": {
                      "used_percent": 25,
                      "limit_window_seconds": 86400,
                      "reset_at": 1785611900
                    },
                    "secondary_window": {
                      "used_percent": 60,
                      "limit_window_seconds": 604800,
                      "reset_at": 1786216700
                    }
                  }
                }
              ],
              "rate_limit_reset_credits": {"available_count": 2}
            }
            """.trimIndent(),
        )

        assertEquals(1, snapshot.additionalLimits.size)
        val spark = snapshot.additionalLimits.single()
        assertEquals("codex_bengalfox", spark.feature)
        assertEquals("GPT-5.3-Codex-Spark", spark.name)
        assertEquals(listOf(75, 40), spark.windows.map { it.remainingPercent })
        assertEquals(listOf(86400L, 604800L), spark.windows.map { it.windowSeconds })
        assertEquals(2, snapshot.availableResetCount)
    }

    @Test
    fun `missing reset credit summary means zero available resets`() {
        val snapshot = parser.parse(
            """
            {
              "rate_limit": {
                "primary_window": {"used_percent": 0}
              }
            }
            """.trimIndent(),
        )

        assertEquals(0, snapshot.availableResetCount)
    }

    @Test(expected = UsageParseException::class)
    fun `missing primary window is rejected`() {
        parser.parse("""{"plan_type":"pro","rate_limit":null}""")
    }
}
