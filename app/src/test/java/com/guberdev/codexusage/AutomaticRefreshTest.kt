package com.guberdev.codexusage

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticRefreshTest {
    @Test
    fun `unlock triggers refresh`() {
        assertTrue(RefreshTrigger.shouldRefreshOnBroadcast(Intent.ACTION_USER_PRESENT))
        assertFalse(RefreshTrigger.shouldRefreshOnBroadcast(Intent.ACTION_BOOT_COMPLETED))
    }

    @Test
    fun `only network recovery triggers refresh`() {
        val online = NetworkRecoveryTracker(initiallyConnected = true)
        assertFalse(online.onAvailable())
        online.onLost()
        assertTrue(online.onAvailable())

        val offline = NetworkRecoveryTracker(initiallyConnected = false)
        assertTrue(offline.onAvailable())
    }
}
