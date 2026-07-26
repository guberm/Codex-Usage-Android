package com.guberdev.codexusage

import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkRetryTest {
    @Test
    fun `unknown host is retried up to three attempts`() {
        var attempts = 0

        val result = retryUnknownHost(delayMillis = 0) {
            attempts++
            if (attempts < 3) throw UnknownHostException("auth.openai.com")
            "connected"
        }

        assertEquals("connected", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `device authorization polling survives a temporary DNS failure`() {
        var now = 0L
        var attempts = 0
        var networkWaits = 0

        val result = pollUntilAuthorized(
            deadlineMillis = 100,
            intervalMillis = 1,
            nowMillis = { now },
            sleep = { now += it },
            onUnknownHost = { networkWaits++ },
        ) {
            attempts++
            when (attempts) {
                1 -> throw UnknownHostException("auth.openai.com")
                2 -> null
                else -> "approved"
            }
        }

        assertEquals("approved", result)
        assertEquals(3, attempts)
        assertEquals(1, networkWaits)
    }
}
