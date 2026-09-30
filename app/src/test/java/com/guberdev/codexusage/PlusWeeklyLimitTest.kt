package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlusWeeklyLimitTest {
    @Test
    fun `plus weekly balance reaches displays independently of five hour balance`() {
        val snapshot = UsageParser().parse(
            """{
                "plan_type": "plus",
                "rate_limit": {
                    "primary_window": {"used_percent": 25, "limit_window_seconds": 18000, "reset_at": 1785611900},
                    "secondary_window": {"used_percent": 60, "limit_window_seconds": 604800, "reset_at": 1786216700}
                }
            }""",
        )

        assertEquals("75%", WidgetDisplay.percent(snapshot))
        assertEquals(UsageWindow(40, 1786216700L, 604800L), snapshot.secondary)
        assertTrue(MonitorDisplay.content(snapshot).contains("Weekly 40% left"))
        assertTrue(WidgetDisplay.reset(snapshot).contains("Weekly 40% left"))
    }

    @Test
    fun `missing or null secondary window does not invent a weekly balance`() {
        for (secondary in listOf("", ",\"secondary_window\":null")) {
            val snapshot = UsageParser().parse(
                """{"rate_limit":{"primary_window":{"used_percent":25}$secondary}}""",
            )
            assertNull(snapshot.secondary)
            assertEquals("Spark unavailable", MonitorDisplay.content(snapshot))
            assertEquals("Reset: —", WidgetDisplay.reset(snapshot))
        }
    }
}
