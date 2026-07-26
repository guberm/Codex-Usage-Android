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
}
