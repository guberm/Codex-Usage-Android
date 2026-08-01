package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDisplayTest {
    @Test
    fun `widget shows the remaining percentage`() {
        val snapshot = UsageSnapshot(
            planType = "plus",
            primary = UsageWindow(remainingPercent = 53, resetAtEpochSeconds = null),
            additionalLimits = emptyList(),
            creditBalance = null,
        )

        assertEquals("53%", WidgetDisplay.percent(snapshot))
        assertEquals(53, WidgetDisplay.progress(snapshot))
        assertEquals("Reset: —", WidgetDisplay.reset(snapshot))
    }

    @Test
    fun `widget prompts for sign in without usage data`() {
        assertEquals("—", WidgetDisplay.percent(null))
        assertEquals(0, WidgetDisplay.progress(null))
        assertEquals("Tap to sign in", WidgetDisplay.reset(null))
    }
}
