package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPolicyTest {
    @Test
    fun `background refresh and change alerts use fixed thresholds`() {
        assertEquals(15L, RefreshPolicy.BACKGROUND_INTERVAL_MINUTES)
        assertEquals(1, RefreshPolicy.NOTIFY_CHANGE_PERCENT)
    }
}
