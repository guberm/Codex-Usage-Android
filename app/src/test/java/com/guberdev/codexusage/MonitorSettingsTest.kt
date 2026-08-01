package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorSettingsTest {
    @Test
    fun `check intervals include quarter-hour choices`() {
        assertEquals(
            listOf(15, 30, 45, 60, 120, 240, 360, 720, 1440),
            MonitorSettingsStore.CHECK_MINUTE_OPTIONS,
        )
        assertEquals("15 min", checkIntervalLabel(15))
        assertEquals("1 hour", checkIntervalLabel(60))
        assertEquals("2 hours", checkIntervalLabel(120))
    }
}
