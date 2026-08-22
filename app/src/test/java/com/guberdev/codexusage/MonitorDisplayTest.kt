package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorDisplayTest {
    @Test
    fun `persistent notification shows the remaining percentage`() {
        val snapshot = UsageSnapshot(
            planType = "plus",
            primary = UsageWindow(remainingPercent = 33, resetAtEpochSeconds = null),
            additionalLimits = listOf(
                AdditionalUsageLimit(
                    feature = "codex_bengalfox",
                    name = "GPT-5.3-Codex-Spark",
                    windows = listOf(
                        UsageWindow(90, null, 86400),
                        UsageWindow(71, null, 604800),
                    ),
                ),
            ),
            creditBalance = null,
            availableResetCount = 1,
        )

        assertEquals("Codex 33% left · 1 reset", MonitorDisplay.title(snapshot))
        assertEquals("Spark: Daily 90% left · Weekly 71% left", MonitorDisplay.content(snapshot))
        assertEquals("33%", MonitorDisplay.shortCriticalText(snapshot))
        assertEquals("Codex Usage monitor", MonitorDisplay.title(null))
        assertEquals("Waiting for the first check", MonitorDisplay.content(null))
        assertEquals("Codex", MonitorDisplay.shortCriticalText(null))
    }
}
