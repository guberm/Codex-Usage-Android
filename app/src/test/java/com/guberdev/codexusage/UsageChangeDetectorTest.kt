package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageChangeDetectorTest {
    @Test
    fun `first observation does not notify`() {
        assertNull(UsageChangeDetector.detect(previousRemaining = null, currentRemaining = 55))
    }

    @Test
    fun `one point decrease notifies`() {
        assertEquals(-1, UsageChangeDetector.detect(previousRemaining = 55, currentRemaining = 54))
    }

    @Test
    fun `one point increase notifies`() {
        assertEquals(1, UsageChangeDetector.detect(previousRemaining = 54, currentRemaining = 55))
    }

    @Test
    fun `unchanged rounded percentage does not notify`() {
        assertNull(UsageChangeDetector.detect(previousRemaining = 55, currentRemaining = 55))
    }

    @Test
    fun `change below configured threshold does not notify`() {
        assertNull(
            UsageChangeDetector.detect(
                previousRemaining = 55,
                currentRemaining = 52,
                minimumChange = 5,
            ),
        )
    }

    @Test
    fun `change at configured threshold notifies with actual delta`() {
        assertEquals(
            -5,
            UsageChangeDetector.detect(
                previousRemaining = 55,
                currentRemaining = 50,
                minimumChange = 5,
            ),
        )
    }

    @Test
    fun `small decreases accumulate from the last notification`() {
        var baseline = 55

        for (current in 54 downTo 51) {
            val decision = UsageChangeDetector.evaluate(baseline, current, minimumChange = 5)
            assertNull(decision.delta)
            baseline = decision.nextBaseline
        }

        val decision = UsageChangeDetector.evaluate(baseline, 50, minimumChange = 5)
        assertEquals(-5, decision.delta)
        assertEquals(50, decision.nextBaseline)
    }

    @Test
    fun `small increases accumulate from the last notification`() {
        var baseline = 50

        for (current in 51..54) {
            val decision = UsageChangeDetector.evaluate(baseline, current, minimumChange = 5)
            assertNull(decision.delta)
            baseline = decision.nextBaseline
        }

        val decision = UsageChangeDetector.evaluate(baseline, 55, minimumChange = 5)
        assertEquals(5, decision.delta)
        assertEquals(55, decision.nextBaseline)
    }
}
