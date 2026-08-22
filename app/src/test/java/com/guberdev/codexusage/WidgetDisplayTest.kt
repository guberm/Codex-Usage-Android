package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDisplayTest {
    @Test
    fun `widget shows the remaining percentage`() {
        val snapshot = UsageSnapshot(
            planType = "plus",
            primary = UsageWindow(remainingPercent = 53, resetAtEpochSeconds = null),
            additionalLimits = listOf(
                AdditionalUsageLimit(
                    feature = "codex_bengalfox",
                    name = "GPT-5.3-Codex-Spark",
                    windows = listOf(
                        UsageWindow(80, null, 86400),
                        UsageWindow(45, null, 604800),
                    ),
                ),
            ),
            creditBalance = null,
            availableResetCount = 2,
        )

        assertEquals("53%", WidgetDisplay.percent(snapshot))
        assertEquals(53, WidgetDisplay.progress(snapshot))
        assertEquals("Reset: —", WidgetDisplay.reset(snapshot))
        assertEquals("Daily 80% left · Weekly 45% left", WidgetDisplay.spark(snapshot))
        assertEquals("2 resets", WidgetDisplay.manualResets(snapshot))
    }

    @Test
    fun `widget prompts for sign in without usage data`() {
        assertEquals("—", WidgetDisplay.percent(null))
        assertEquals(0, WidgetDisplay.progress(null))
        assertEquals("Tap to sign in", WidgetDisplay.reset(null))
        assertEquals("Spark unavailable", WidgetDisplay.spark(null))
        assertEquals("0 resets", WidgetDisplay.manualResets(null))
    }
}
