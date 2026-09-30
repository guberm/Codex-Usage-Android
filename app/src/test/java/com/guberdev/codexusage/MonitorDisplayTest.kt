package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorDisplayTest {
    @Test
    fun `notification title and compact status show both usage windows`() {
        val snapshot = UsageSnapshot(
            planType = "plus",
            primary = UsageWindow(remainingPercent = 33, resetAtEpochSeconds = null, windowSeconds = 18_000),
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
            secondary = UsageWindow(remainingPercent = 71, resetAtEpochSeconds = null, windowSeconds = 604_800),
        )

        assertEquals("5h 33% · Weekly 71%", MonitorDisplay.title(snapshot))
        assertEquals("Spark: Daily 90% left · Weekly 71% left", MonitorDisplay.content(snapshot))
        assertEquals("33/71", MonitorDisplay.shortCriticalText(snapshot))
        assertEquals("Codex 33% left · 1 reset", MonitorDisplay.title(snapshot.copy(secondary = null)))
        assertEquals("33%", MonitorDisplay.shortCriticalText(snapshot.copy(secondary = null)))
        assertEquals("Codex Usage monitor", MonitorDisplay.title(null))
        assertEquals("Waiting for the first check", MonitorDisplay.content(null))
        assertEquals("Codex", MonitorDisplay.shortCriticalText(null))
    }
}
