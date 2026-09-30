package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlusWeeklyLimitTest {
    @Test
    fun `plus weekly balance reaches displays independently of five hour balance`() {
        val snapshot = UsageParser().parse(
            """{
                "plan_type": "plus",
                "rate_limit": {
                    "primary_window": {"used_percent": 25, "limit_window_seconds": 18000},
                    "secondary_window": {"used_percent": 60, "limit_window_seconds": 604800}
                }
            }""",
        )

        assertEquals("75%", WidgetDisplay.percent(snapshot))
        assertTrue(MonitorDisplay.content(snapshot).contains("Weekly 40% left"))
        assertTrue(WidgetDisplay.reset(snapshot).contains("Weekly 40% left"))
    }
}
