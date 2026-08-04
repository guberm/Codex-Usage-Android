package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorDisplayTest {
    @Test
    fun `persistent notification shows the remaining percentage`() {
        val snapshot = UsageSnapshot(
            planType = "plus",
            primary = UsageWindow(remainingPercent = 33, resetAtEpochSeconds = null),
            additionalLimits = emptyList(),
            creditBalance = null,
        )

        assertEquals("33% Codex remaining", MonitorDisplay.title(snapshot))
        assertEquals("33%", MonitorDisplay.shortCriticalText(snapshot))
        assertEquals("Codex Usage monitor", MonitorDisplay.title(null))
        assertEquals("Codex", MonitorDisplay.shortCriticalText(null))
    }
}
