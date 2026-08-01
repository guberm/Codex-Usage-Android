package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorDisplayTest {
    @Test
    fun `status bar chip shows the remaining percentage`() {
        val snapshot = UsageSnapshot(
            planType = "plus",
            primary = UsageWindow(remainingPercent = 53, resetAtEpochSeconds = null),
            additionalLimits = emptyList(),
            creditBalance = null,
        )

        assertEquals("53%", MonitorDisplay.shortCriticalText(snapshot))
        assertEquals("Codex", MonitorDisplay.shortCriticalText(null))
    }
}
